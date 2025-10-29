/*
 * Rubus is a protocol for video and audio streaming and
 * the client and server reference implementations.
 * Copyright (C) 2025 Yegore Vlussove
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

package frontend.converters;

import frontend.adapters.ArraySeekableByteChannel;
import frontend.models.MediaFetch;
import jakarta.annotation.Nonnull;
import org.bson.*;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.io.BasicOutputBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;

/**
 * Converts {@link MediaFetch} from/into binary format.
 */
public class MediaFetchBinaryConverter implements BinaryConverter<MediaFetch> {

	@Nonnull
	@Override
	public SeekableByteChannel convert(@Nonnull MediaFetch input) throws IOException {
		BsonDocument bsonDocument = new BsonDocument();
		bsonDocument.put("id", new BsonString(input.id()));
		bsonDocument.put("offset", new BsonInt32(input.offset()));

		ArrayList<BsonBinary> video = new ArrayList<>();
		for (byte[] channel: input.video()) {
			video.add(new BsonBinary(channel));
		}
		bsonDocument.put("video", new BsonArray(video));

		ArrayList<BsonBinary> audio = new ArrayList<>();
		for (byte[] channel: input.audio()) {
			audio.add(new BsonBinary(channel));
		}
		bsonDocument.put("audio", new BsonArray(audio));

		try (
			BasicOutputBuffer basicOutputBuffer = new BasicOutputBuffer();
			BsonBinaryWriter bsonBinaryWriter = new BsonBinaryWriter(basicOutputBuffer)
		) {
			BsonDocumentCodec bsonDocumentCodec = new BsonDocumentCodec();
			bsonDocumentCodec.encode(bsonBinaryWriter, bsonDocument, EncoderContext.builder().build());
			return new ArraySeekableByteChannel(basicOutputBuffer.toByteArray());
		}
	}

	@Nonnull
	@Override
	public MediaFetch convert(@Nonnull SeekableByteChannel input) throws IOException {
		ByteBuffer byteBuffer = ByteBuffer.allocate((int) input.size());
		input.read(byteBuffer);
		byteBuffer.flip();

		try (BsonBinaryReader bsonBinaryReader = new BsonBinaryReader(byteBuffer)) {
			BsonDocumentCodec bsonDocumentCodec = new BsonDocumentCodec();
			BsonDocument bsonDocument = bsonDocumentCodec.decode(bsonBinaryReader, DecoderContext.builder().build());
			byte[][] video = bsonDocument
				.getArray("video")
				.stream()
				.map(bsonValue -> bsonValue.asBinary().getData())
				.toArray(byte[][]::new);
			byte[][] audio = bsonDocument
				.getArray("audio")
				.stream()
				.map(bsonValue -> bsonValue.asBinary().getData())
				.toArray(byte[][]::new);
			return new MediaFetch(
				bsonDocument.getString("id").getValue(),
				bsonDocument.getInt32("offset").getValue(),
				video,
				audio
			);
		}
	}
}
