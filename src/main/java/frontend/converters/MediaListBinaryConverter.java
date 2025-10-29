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

package frontend.converters;

import frontend.adapters.ArraySeekableByteChannel;
import frontend.models.MediaList;
import jakarta.annotation.Nonnull;
import org.bson.*;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.io.BasicOutputBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.HashMap;

/**
 * Converts {@link MediaList} to/from binary format.
 */
public class MediaListBinaryConverter implements BinaryConverter<MediaList> {

	@Nonnull
	@Override
	public SeekableByteChannel convert(@Nonnull MediaList input) {
		BsonDocument bsonDocument = new BsonDocument();
		BsonDocument media = new BsonDocument(
			input
				.media()
				.entrySet()
				.stream()
				.map(entry -> new BsonElement(entry.getKey(), new BsonString(entry.getValue())))
				.toList()
		);
		bsonDocument.put("media", media);

		try (
			BasicOutputBuffer basicOutputBuffer = new BasicOutputBuffer();
			BsonBinaryWriter bsonBinaryWriter = new BsonBinaryWriter(basicOutputBuffer);
		) {
			BsonDocumentCodec bsonDocumentCodec = new BsonDocumentCodec();
			bsonDocumentCodec.encode(bsonBinaryWriter, bsonDocument, EncoderContext.builder().build());
			return new ArraySeekableByteChannel(basicOutputBuffer.toByteArray());
		}
	}

	@Nonnull
	@Override
	public MediaList convert(@Nonnull SeekableByteChannel input) throws IOException {
		ByteBuffer byteBuffer = ByteBuffer.allocate((int) input.size());
		input.read(byteBuffer);
		byteBuffer.flip();

		try (BsonBinaryReader bsonBinaryReader = new BsonBinaryReader(byteBuffer)) {
			BsonDocumentCodec bsonDocumentCodec = new BsonDocumentCodec();
			BsonDocument bsonDocument = bsonDocumentCodec.decode(bsonBinaryReader, DecoderContext.builder().build());
			BsonDocument media = bsonDocument.getDocument("media");
			return new MediaList(
				media
					.entrySet()
					.stream()
					.reduce(
						new HashMap<>(),
						(map, entry) -> {
							map.put(entry.getKey(), entry.getValue().asString().getValue());
							return map;
						},
						(map1, map2) -> {
							map1.putAll(map2);
							return map1;
						}
					)
			);
		}
	}
}
