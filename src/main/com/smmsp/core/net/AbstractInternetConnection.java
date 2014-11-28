/*
 * InternetConnection.java
 * 
 * Copyright (C) 2012 Sean P Madden
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * @author sean
 * 
 */
public abstract class AbstractInternetConnection {

	/**
	 * Subclasses override this method to open the connection and get the input
	 * stream.
	 * 
	 * @return
	 */
	public abstract InputStream getDataStream();

	/**
	 * Simplifies calling 'getDataStream' and routing that to a file.
	 * 
	 * @param f
	 *            The file to save the data returned by {@link #getDataStream()}
	 *            If this file already exists, it will overwrite all data inside
	 *            it.
	 * @return True if file was written properly
	 * @throws IOException 
	 */
	public boolean saveToFile(final File f) throws IOException {
		return Files.copy(getDataStream(), 
				f.toPath(), 
				StandardCopyOption.REPLACE_EXISTING) > 0;
		
	}

	/**
	 * Shorthand for {@link #saveToFile(File))
	 * @param f A string path to the file
	 * @return True if the file was written properly
	 * @throws IOException
	 */
	public boolean saveToFile(final String f) throws IOException {
		return saveToFile(new File(f));
	}
}
