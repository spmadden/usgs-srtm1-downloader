/*
 * HTTPCachedFile.java
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
import java.net.ProtocolException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.apache.log4j.Logger;

import com.smmsp.core.utils.OSAPI;

/**
 * @author sean
 *
 */
public class HTTPCachedFile extends Cachable {

	private static final Logger log = Logger.getLogger(HTTPCachedFile.class);
	
	private final String filename;
	
	private final String url;
	
	/**
	 * Constructor!
	 * @param filename
	 * @param url
	 */
	public HTTPCachedFile(
			final String filename,
			final String url){
		this.filename = filename;
		this.url = url;
		
	}
	
	/* (non-Javadoc)
	 * @see com.smmsp.core.net.Cachable#updateCache()
	 */
	@Override
	public void updateCache() throws CacheException {
		Path fullCachedFile = getPathToCache();

		try {
			HTTPConnection conn = new HTTPConnection(url);

			ReadableByteChannel ftpChan = Channels.newChannel(conn
					.getDataStream());

			// create the file if it doesn't exist and
			// hose it if it does
			FileChannel cachedFile = FileChannel.open(fullCachedFile,
					StandardOpenOption.CREATE, StandardOpenOption.WRITE,
					StandardOpenOption.TRUNCATE_EXISTING);

			// pipe it to the file.
			cachedFile.transferFrom(ftpChan, 0, Integer.MAX_VALUE);
		} catch (ProtocolException e) {
			e.printStackTrace();
			log.error("Error with FTP download from IANA.", e);
		} catch (IOException e) {
			e.printStackTrace();
			log.error("Error file file IO.", e);
		}

	}

	/* (non-Javadoc)
	 * @see com.smmsp.core.net.Cachable#getPathToCache()
	 */
	@Override
	public Path getPathToCache() throws CacheException {
		Path fullCachedFile = Paths.get(OSAPI.getCacheDirectory().toString(),
				filename);
		return fullCachedFile;
	}

}
