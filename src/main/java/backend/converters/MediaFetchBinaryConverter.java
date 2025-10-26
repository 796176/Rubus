/*
 * Rubus is an application layer protocol for video and audio streaming and
 * the client and server reference implementations.
 * Copyright (C) 2024 Yegore Vlussove
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

package backend.converters;

import backend.adapters.ArraySeekableByteChannel;
import backend.models.MediaFetch;
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
import java.util.UUID;

/**
 * Converts {@link MediaFetch} from/into binary format.
 */
public class MediaFetchBinaryConverter implements BinaryConverter<MediaFetch> {

	@Nonnull
	@Override
	public SeekableByteChannel convert(@Nonnull MediaFetch input) throws IOException {
		BsonDocument bsonDocument = new BsonDocument();
		bsonDocument.put("id", new BsonString(input.id().toString()));
		bsonDocument.put("offset", new BsonInt32(input.offset()));

		ArrayList<BsonBinary> video = new ArrayList<>(input.video().length);
		for (SeekableByteChannel channel: input.video()) {
			video.add(new BsonBinary(toByteArray(channel)));
		}
		bsonDocument.put("video", new BsonArray(video));

		ArrayList<BsonBinary> audio = new ArrayList<>(input.audio().length);
		for (SeekableByteChannel channel: input.audio()) {
			audio.add(new BsonBinary(toByteArray(channel)));
		}
		bsonDocument.put("audio", new BsonArray(audio));

		try(
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

			SeekableByteChannel[] video = bsonDocument
				.getArray("video")
				.stream()
				.map(bsonValue -> new ArraySeekableByteChannel(bsonValue.asBinary().getData()))
				.toArray(SeekableByteChannel[]::new);
			SeekableByteChannel[] audio = bsonDocument
				.getArray("audio")
				.stream()
				.map(bsonValue -> new ArraySeekableByteChannel(bsonValue.asBinary().getData()))
				.toArray(SeekableByteChannel[]::new);
			return new MediaFetch(
				UUID.fromString(bsonDocument.getString("id").getValue()),
				bsonDocument.getInt32("offset").getValue(),
				video,
				audio
			);
		}
	}

	private byte[] toByteArray(SeekableByteChannel sbc) throws IOException {
		byte[] output = new byte[(int) sbc.size()];
		int byteRead = 0;
		do {
			byteRead += sbc.read(ByteBuffer.wrap(output, byteRead, output.length - byteRead));
		} while (byteRead < output.length);
		return output;
	}
}
