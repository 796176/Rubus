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

package backend.converters;

import backend.adapters.ArraySeekableByteChannel;
import backend.models.MediaInfo;
import jakarta.annotation.Nonnull;
import org.bson.*;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.io.BasicOutputBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.UUID;

/**
 * Converts {@link MediaInfo} from/into binary format.
 */
public class MediaInfoBinaryConverter implements BinaryConverter<MediaInfo> {

	@Nonnull
	@Override
	public SeekableByteChannel convert(@Nonnull MediaInfo input) {
		BsonDocument bsonDocument = new BsonDocument();
		bsonDocument.put("id", new BsonString(input.id().toString()));
		bsonDocument.put("title", new BsonString(input.title()));
		bsonDocument.put("duration", new BsonInt32(input.duration()));

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
	public MediaInfo convert(@Nonnull SeekableByteChannel input) throws IOException {
		ByteBuffer byteBuffer = ByteBuffer.allocate((int) input.size());
		input.read(byteBuffer);
		byteBuffer.flip();
		try (BsonBinaryReader bsonBinaryReader = new BsonBinaryReader(byteBuffer)) {
			BsonDocumentCodec bsonDocumentCodec = new BsonDocumentCodec();
			BsonDocument bsonDocument = bsonDocumentCodec.decode(bsonBinaryReader, DecoderContext.builder().build());

			return new MediaInfo(
				UUID.fromString(bsonDocument.getString("id").getValue()),
				bsonDocument.getString("title").getValue(),
				bsonDocument.getInt32("duration").getValue()
			);
		}
	}
}
