/*
 * Cachable.java
 * 
 * Copyright (C) 2013 Sean P Madden
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * If you would like to license this code under the GNU LGPL, please
 * see http://www.seanmadden.net/licensing for details.
 */
package com.smmsp.core.net;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import org.apache.log4j.Logger;

import com.smmsp.core.utils.OSAPI;

/**
 * This interface represents an object that contains a cache, and can update
 * that cache.
 * 
 * @author sean
 * 
 */
public abstract class Cachable {

	/**
	 * Maximum length before the cache expires (1 week in seconds)
	 */
	private static final int MAX_CACHE_TIMEOUT = 604800;
	private static final Logger log = Logger.getLogger(Cachable.class);

	/**
	 * An exception to be thrown when there is an issue with the cache
	 * 
	 * @author sean
	 * 
	 */
	public static class CacheException extends RuntimeException {
		/**
		 * 
		 */
		private static final long serialVersionUID = 3850829271771966035L;

		/**
		 * Constructor
		 * 
		 * @param msg
		 */
		public CacheException(String msg) {
			super(msg);
		}

		/**
		 * Constructor
		 * 
		 * @param msg
		 * @param err
		 */
		public CacheException(String msg, Throwable err) {
			super(msg, err);
		}

	}

	/**
	 * Instructs this cachable to update it's cache.
	 */
	public abstract void updateCache() throws CacheException;

	/**
	 * Does this cachable need to update it's cache?
	 * 
	 * @return True if the cache needs updating.
	 */
	public boolean cacheNeedsUpdate() throws CacheException {
		try {
			OSAPI.ensureCacheDirExists();
		} catch (IOException e) {
			// This shouldn't happen, but if it does - log an error.
			log.error("Could not create cache directory.", e);
			throw new CacheException("Could not create cache directory.", e);
		}

		boolean needsDownload = false;
		Path fullCachedFile = getPathToCache();

		if (Files.notExists(fullCachedFile)) {
			needsDownload = true;
		} else {
			try {
				FileTime time = Files.getLastModifiedTime(fullCachedFile);
				FileTime now = FileTime.fromMillis(System.currentTimeMillis()
						+ MAX_CACHE_TIMEOUT * 1000);

				if (now.compareTo(time) < 0) {
					// cache is invalidated.
					needsDownload = true;
				}
			} catch (IOException e) {
				log.error(
						"Unable to get file modification time on leaps cache",
						e);
				throw new CacheException(
						"Unable to get file modification time on leaps cache",
						e);
			}
		}

		return needsDownload;
	}

	/**
	 * Returns the path to this cachable's cache.
	 * 
	 * @return
	 * @throws CacheException
	 */
	public abstract Path getPathToCache() throws CacheException;
}
