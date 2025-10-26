package backend.models;

import jakarta.annotation.Nonnull;

import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.util.UUID;

/**
 * The Media interface provides access to media-specific information.
 */
public interface Media {

	/**
	 * Returns the media id.
	 * @return the media id
	 */
	@Nonnull
	UUID getID();

	/**
	 * Returns the title of the media.
	 * @return the title of the media
	 */
	@Nonnull
	String getTitle();

	/**
	 * Returns the duration of the media.
	 * @return the duration of the media
	 */
	int getDuration();

	/**
	 * Returns the URI of the media content.
	 * @return the URI of the media content
	 */
	@Nonnull
	URI getContentURI();

	/**
	 * Retrieves audio clips of the specified range.
	 * @param offset how many clips to skip
	 * @param amount how many clips to retrieve
	 * @return an ordered array of {@link SeekableByteChannel} instances containing audio clips
	 */
	@Nonnull
	SeekableByteChannel[] retrieveAudioClips(int offset, int amount);

	/**
	 * Retrieves video clips of the specified range.
	 * @param offset how many clips to skip
	 * @param amount how many clips to retrieve
	 * @return an ordered array of {@link SeekableByteChannel} instances containing video clips
	 */
	@Nonnull
	SeekableByteChannel[] retrieveVideoClips(int offset, int amount);
}
