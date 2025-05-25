/*
 * Rubus is an application level protocol for video and audio streaming and
 * the client and server reference implementations.
 * Copyright (C) 2024-2025 Yegore Vlussove
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

#include "frontend_decoders_FfmpegJniVideoDecoder.h"

#include <libavcodec/avcodec.h>
#include <libavcodec/packet.h>
#include <libavformat/avformat.h>
#include <libavutil/imgutils.h>
#include <libavutil/opt.h>
#include <libswscale/swscale.h>

#include <stdio.h>
#include <string.h>

/**
 * context0 stores the information associated with the current video stream and its type is 0.
 * frame is an allocated AVFrame to perform video decoding
 * packet is an allocated AVPacket to perform video decoding
 * buffer is an allocated byte array to store intermediate data while performing video decoding
 * codec_context is an instance of AVCodecContext
 * sws_context is an instance of SwsContext
 * frame_rate is the video stream frame-rate or <=0 if it's unknown
 */
struct context0 {
	AVFrame *frame;
	AVPacket *packet;
	uint8_t *buffer;
	AVCodecContext *codec_context;
	struct SwsContext *sws_context;
	uint64_t frame_rate;
};

/**
 * Instantiates an AVFormatContext using data in media to determine the video format.
 * @param context the address the instance of AVFormatContext is written into
 * @param media the encoded video
 * @param media_size the size of media
 * @return 0 on success, <0 on error
 */
static int retrieve_video_format_context(AVFormatContext **context, uint8_t *media, size_t media_size) {
	AVIOContext *io_context = avio_alloc_context(media, media_size, 0, NULL, NULL, NULL, NULL);
	*context = avformat_alloc_context();
	(*context)->pb = io_context;
	int error_code = avformat_open_input(context, NULL, NULL, NULL);
	if (error_code) return error_code;
	return avformat_find_stream_info(*context, NULL);
}

/**
 * Instantiates an AVCodecContext given the specified parameters.
 * @param context the address the instance of AVCodecContext is written into
 * @param params the parameters necessary to initialize AVCodecContext
 * @return 0 on success or <0 on error
 */
static int retrieve_codec_context(AVCodecContext **context, AVCodecParameters *params) {
	const AVCodec *codec = avcodec_find_decoder(params->codec_id);
	if (!codec) return -1;
	*context = avcodec_alloc_context3(codec);
	int error_code = avcodec_parameters_to_context(*context, params);
	if (error_code) return error_code;
	return avcodec_open2(*context, codec, NULL);
}
/**
 * Instantiates a java Image.
 * @param arr a java array that stores pixels represented as ints
 * @param env java environment
 * @param width the frame width
 * @param height the frame height
 * @return the java Image
 */
static jobject create_java_frame(jintArray arr, JNIEnv *env, int width, int height) {
	// ColorSpace color_space_obj = ColorSpace.getInstance(ColorSpace.CS_sRGB)
	jclass color_space_cls = (*env)->FindClass(env, "java/awt/color/ColorSpace");
	jmethodID color_space_fm =
		(*env)->GetStaticMethodID(env, color_space_cls, "getInstance", "(I)Ljava/awt/color/ColorSpace;");
	jobject color_space_obj = (*env)->CallStaticObjectMethod(env, color_space_cls, color_space_fm, 1000);

	// DirectColorModel direct_color_model_obj = new DirectColorModel(24, 0xff0000, 0xff, 0xff)
	jclass direct_color_model_cls = (*env)->FindClass(env, "java/awt/image/DirectColorModel");
	jmethodID direct_color_model_init = (*env)->GetMethodID(env, direct_color_model_cls, "<init>", "(IIII)V");
	jobject direct_color_model_obj =
		(*env)->NewObject(env, direct_color_model_cls, direct_color_model_init, 24, 0xff0000, 0xff00, 0xff);

	// SinglePixelPackedSampleModel spp_sample_model_obj =
	//     new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT, width, height, new int[]{ 0xff0000, 0xff00, 0xff })
	jclass spp_sample_model_cls = (*env)->FindClass(env, "java/awt/image/SinglePixelPackedSampleModel");
	jmethodID spp_sample_model_init = (*env)->GetMethodID(env, spp_sample_model_cls, "<init>", "(III[I)V");
	jintArray bit_masks = (*env)->NewIntArray(env, 3);
	(*env)->SetIntArrayRegion(env, bit_masks, 0, 3, (int[]) { 0xff0000, 0xff00, 0xff });
	jobject spp_sample_model_obj =
		(*env)->NewObject(env, spp_sample_model_cls, spp_sample_model_init, 3, width, height, bit_masks);

	// Point point_obj = new Point(0, 0)
	jclass point_cls = (*env)->FindClass(env, "java/awt/Point");
	jmethodID point_init = (*env)->GetMethodID(env, point_cls, "<init>", "(II)V");
	jobject point_obj = (*env)->NewObject(env, point_cls, point_init, 0, 0);

	// DataBufferInt data_buffer_int_obj = new DataBufferInt(arr, width * height)
	jclass data_buffer_int_cls = (*env)->FindClass(env, "java/awt/image/DataBufferInt");
	jmethodID data_buffer_int_init = (*env)->GetMethodID(env, data_buffer_int_cls, "<init>", "([II)V");
	jobject data_buffer_int_obj =
		(*env)->NewObject(env, data_buffer_int_cls, data_buffer_int_init, arr, width * height);

	// WritableRaster raster_obj = Raster.createWritableRaster(spp_sample_model_obj, data_buffer_int_obj, point_obj)
	jclass raster_cls = (*env)->FindClass(env, "java/awt/image/Raster");
	jmethodID raster_fm =
		(*env)->GetStaticMethodID(
			env,
			raster_cls,
			"createWritableRaster",
			"(Ljava/awt/image/SampleModel;Ljava/awt/image/DataBuffer;Ljava/awt/Point;)Ljava/awt/image/WritableRaster;"
		);
	jobject raster_obj = (*env)->CallStaticObjectMethod(
		env, raster_cls, raster_fm, spp_sample_model_obj, data_buffer_int_obj, point_obj
	);

	// return new BufferedImage(direct_color_model_obj, raster_obj, false, null)
	jclass buffered_image_cls = (*env)->FindClass(env, "java/awt/image/BufferedImage");
	jmethodID buffered_image_init =
		(*env)->GetMethodID(
			env,
			buffered_image_cls,
			"<init>",
			"(Ljava/awt/image/ColorModel;Ljava/awt/image/WritableRaster;ZLjava/util/Hashtable;)V"
		);
	return (*env)->NewObject(
		env, buffered_image_cls, buffered_image_init, direct_color_model_obj, raster_obj, JNI_FALSE, NULL
	);
}

/**
 * Converts an AVFrame into a java Image.
 * @param frame an AVFrame content of which needs to be represented as a java Image
 * @param context an instance of SwsContext
 * @param buf a buffer to store intermediate data with the size of width * height * 3
 * @param width the frame width
 * @param height the frame height
 * @return the instance of Image
 */
static jobject convert_to_java_frame(
	AVFrame *frame, JNIEnv *env, struct SwsContext *context, uint8_t *buf, int width, int height
) {
	sws_scale(
		context, (const uint8_t* const *)(frame->data), frame->linesize, 0, height, &buf, (int[]){ width * 3, 0, 0, 0}
	);
	jintArray java_pxl_arr = (*env)->NewIntArray(env, width * height);
	jint *pixels = (*env)->GetIntArrayElements(env, java_pxl_arr, NULL);
	for (int i = 0, j = 0; i < width * height * 3; i += 3, j++) {
		pixels[j] =
			(uint32_t)buf[    i] << 16 & 0xff0000 |
			(uint32_t)buf[i + 1] << 8  & 0xff00   |
			(uint32_t)buf[i + 2]       & 0xff;
	}
	(*env)->ReleaseIntArrayElements(env, java_pxl_arr, pixels, 0);
	return create_java_frame(java_pxl_arr, env, width, height);
}

/**
 * Decodes a video clip using the video clip format information and the context data structure information.
 * @param env java environment
 * @param obj the caller
 * @param context_address the memory address of the allocated and initialized context data structure
 * @param context_type the type of the context data structure
 * @param encoded_video the video clip
 * @param offset the number of frames to skip
 * @param total the number of frames to decode
 * @return a java array containing instances of Image
 */
JNIEXPORT jobjectArray JNICALL Java_frontend_decoders_FfmpegJniVideoDecoder_decodeFrames(
	JNIEnv *env,
	jobject obj,
	jlong context_address,
	jint context_type,
	jbyteArray encoded_video,
	jint offset,
	jint total
) {
	jclass exception_class = (*env)->FindClass(env, "common/DecodingException");

	if (context_type == 0) {
		struct context0 *context = (struct context0 *) context_address;

		// retrieving primitives from the array
		jsize encoded_video_size = (*env)->GetArrayLength(env, encoded_video);
		jbyte *encoded_video_data = (*env)->GetByteArrayElements(env, encoded_video, NULL);

		AVFormatContext *format_context;
		int error_code = retrieve_video_format_context(&format_context, encoded_video_data, encoded_video_size);
		if (error_code) {
			avio_context_free(&(format_context->pb));
			avformat_close_input(&format_context);

			char error_mes[256];
			snprintf(error_mes, sizeof(error_mes), "Demuxing failed, error code: %d", error_code);
			(*env)->ThrowNew(env, exception_class, error_mes);
			return NULL;
		}
		// format context has only one video stream
		AVStream *vid_stream = format_context->streams[0];
		int video_width = vid_stream->codecpar->width;
		int video_height = vid_stream->codecpar->height;

		jobject java_frames[total];
		int java_frames_size = 0;
		AVPacket *packet = context->packet;
		while (total > 0) {
			// extracting a packet containing one encoded frame
			error_code = av_read_frame(format_context, packet);
			if (error_code) {
				avio_context_free(&(format_context->pb));
				avformat_close_input(&format_context);

				char error_mes[256];
				snprintf(error_mes, sizeof(error_mes), "AVPacket initialization failed, error code: %d", error_code);
				(*env)->ThrowNew(env, exception_class, error_mes);
				return NULL;
			}

			// sending the packet to the decoding pipeline
			int error_code = avcodec_send_packet(context->codec_context, context->packet);
			if (error_code) {
				avio_context_free(&(format_context->pb));
				avformat_close_input(&format_context);

				char error_mes[256];
				snprintf(error_mes, sizeof(error_mes), "Decoding failed, error code %d", error_code);
				(*env)->ThrowNew(env, exception_class, error_mes);
				return NULL;
			}

			// receiving a decoded frame from the decoding pipeline
			error_code = avcodec_receive_frame(context->codec_context, context->frame);
			if (error_code) {
				av_packet_unref(context->packet);
				avio_context_free(&(format_context->pb));
				avformat_close_input(&format_context);

				char error_mes[216];
				snprintf(error_mes, sizeof(error_mes), "Decoding failed, error code %d", error_code);
				(*env)->ThrowNew(env, exception_class, error_mes);
				return NULL;
			}

			jobject java_frame = convert_to_java_frame(
				context->frame, env, context->sws_context, context->buffer, video_width, video_height
			);
			java_frames[java_frames_size++] = java_frame;
			total--;
        	av_frame_unref(context->frame);
        	av_packet_unref(context->packet);
        }

		avio_context_free(&(format_context->pb));
		avformat_close_input(&format_context);

		// creating a new java array containing all the decoded frames
		jclass image_cls = (*env)->FindClass(env, "java/awt/Image");
		jobjectArray decoded_frames = (*env)->NewObjectArray(env, java_frames_size, image_cls, NULL);
		for (int i = 0; i < java_frames_size; i++) {
			(*env)->SetObjectArrayElement(env, decoded_frames, i, java_frames[i]);
		}

		return decoded_frames;
	}

	char error_mes[216];
	snprintf(error_mes, sizeof(error_mes), "Context %d is not supported", context_type);
	(*env)->ThrowNew(env, exception_class, error_mes);
	return NULL;
}

/**
 * Retrieve the frame-rate from the context. If the frame-rate is unknown the exception is thrown.
 * @param env java environment
 * @param obj the caller
 * @param context_address the memory address of the allocated and initialized context data structure
 * @param context_type the type of the context data structure
 * @return the frame-rate
 */
JNIEXPORT jint JNICALL Java_frontend_decoders_FfmpegJniVideoDecoder_frames (
	JNIEnv *env, jobject obj, jlong context_address, jint context_type
) {
	jclass exception_class = (*env)->FindClass(env, "common/DecodingException");

	if (context_type == 0) {
		struct context0 *context = (struct context0 *)context_address;

		uint64_t frame_rate = context->frame_rate;
		if (frame_rate > 0) return frame_rate;
		(*env)->ThrowNew(env, exception_class, "Unknown frame rate");
		return 0;
	}

	char error_mes[256];
	snprintf(error_mes, sizeof(error_mes), "Context %d is not supported", context_type);
	(*env)->ThrowNew(env, exception_class, error_mes);
	return 0;
}

/**
 * Allocates and initializes a new context data structure of the specified type.
 * @param env java environment
 * @param obj the caller
 * @param encoded_video the video clip
 * @param context_type the type of the context data structure
 * @return the memory address of allocated and initialized data structure
 */
JNIEXPORT jlong JNICALL Java_frontend_decoders_FfmpegJniVideoDecoder_initContext(
	JNIEnv *env, jobject obj, jbyteArray encoded_video, jint context_type
) {
	jclass exception_class = (*env)->FindClass(env, "common/DecodingException");

	if (context_type == 0) {
		struct context0 *context = calloc(1, sizeof(struct context0));

		// extracting primitives from the array
		jsize encoded_video_size = (*env)->GetArrayLength(env, encoded_video);
		jbyte *encoded_video_data = (*env)->GetByteArrayElements(env, encoded_video, NULL);

		AVFormatContext *format_context;
        int error_code = retrieve_video_format_context(&format_context, encoded_video_data, encoded_video_size);
        if (error_code) {
        	char error_mes[256];
        	snprintf(error_mes, sizeof(error_mes), "Demuxing failed, error code: %d", error_code);
        	(*env)->ThrowNew(env, exception_class, error_mes);
        	return (uint64_t)NULL;
        }
        // format context has only one video stream
        AVStream *vid_stream = format_context->streams[0];
        context->frame_rate = vid_stream->nb_frames;
        int video_width = vid_stream->codecpar->width;
        int video_height = vid_stream->codecpar->height;
        enum AVPixelFormat pixel_format = vid_stream->codecpar->format;

        error_code = retrieve_codec_context(&(context->codec_context), vid_stream->codecpar);
		avio_context_free(&(format_context->pb));
		avformat_close_input(&format_context);
        if (error_code) {
        	char error_mes[256];
        	snprintf(error_mes, sizeof(error_mes), "Context initialization failed, error code: %d", error_code);
        	(*env)->ThrowNew(env, exception_class, error_mes);
        	return (uint64_t)NULL;
        }

        context->sws_context =
        	sws_getContext(
				video_width,
				video_height,
				pixel_format,
				video_width,
				video_height,
				AV_PIX_FMT_RGB24,
				SWS_BILINEAR,
				NULL,
				NULL,
				NULL
        	);
        if (!(context->sws_context)) {
        	char error_mes[256];
        	snprintf(error_mes, sizeof(error_mes), "SWS context initialization failed, error code: %d", error_code);
        	(*env)->ThrowNew(env, exception_class, error_mes);
        	return (uint64_t)NULL;
        }

		context->buffer = calloc(video_width * video_height * 3, sizeof(uint8_t));
		context->packet = av_packet_alloc();
        context->frame = av_frame_alloc();

        return (uint64_t)context;
	}

	char error_mes[256];
	snprintf(error_mes, sizeof(error_mes), "Context %d is not supported", context_type);
	(*env)->ThrowNew(env, exception_class, error_mes);
	return (uint64_t)NULL;
}

/**
 * Deallocates the context data structure
 * @param env java environment
 * @param obj the caller
 * @param context_address the memory address of the allocated context data structure
 * @param context_type the type of the context data structure
 */
JNIEXPORT void JNICALL Java_frontend_decoders_FfmpegJniVideoDecoder_freeContext(
	JNIEnv *env, jobject obj, jlong context_address, jint context_type
) {
	if (context_type == 0) {
		struct context0 *context = (struct context0 *) context_address;

		avcodec_free_context(&(context->codec_context));
		av_frame_free(&(context->frame));
		av_packet_free(&(context->packet));
		sws_freeContext(context->sws_context);
		free(context->buffer);
		free(context);
	}
}
