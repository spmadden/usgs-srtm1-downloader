/*
 * FTPConnection.java
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.smmsp.core.utils.ArrayUtils;

/**
 * This class wraps the URL connection applying a bit of FTP protocol logic to
 * make downloading files from FTP easier.
 * 
 * @author sean
 * 
 */
public class FTPConnection extends AbstractInternetConnection {

	private static final Logger LOG = Logger.getLogger(FTPConnection.class);

	private static final Pattern PASV_PATTERN = Pattern.compile(".+\\((.+)\\).*");
	
	private static final int BUFFER_SIZE = 2048;
	
	private String user = "anonymous";
	private String pass = "anonymous";
	private String path = "";
	private String file = "";
	private String server = "";
	private int port = 21;

	public FTPConnection(URL url) throws MalformedURLException {
		if(LOG.isDebugEnabled()){
			LOG.debug("Created FTPConnection Object with URL: " + url);
		}

		if (!"ftp".equals(url.getProtocol().toLowerCase())) {
			throw new MalformedURLException("This is not an FTP url!");
		}
		if (url.getUserInfo() != null) {
			String userinfo = url.getUserInfo();
			String[] parts = userinfo.split(":");
			if (parts.length > 2) {
				throw new MalformedURLException();
			} else if (parts.length == 1) {
				user = userinfo;
			} else if (parts.length == 2) {
				user = parts[0];
				pass = parts[1];
			}
		}
		path = url.getPath();
		int lastSlashPos = path.lastIndexOf('/');
		file = path.substring(lastSlashPos + 1);
		path = path.substring(0, lastSlashPos);
				
		server = url.getHost();

		if (url.getPort() > 0) {
			port = url.getPort();
		}
	}

	public FTPConnection(String url) throws MalformedURLException {
		this(new URL(url));
	}

	@Override
	public InputStream getDataStream() {
		try (
				SocketChannel commandSock = SocketChannel.open();
				SocketChannel dataSock = SocketChannel.open();
			){
			
			commandSock.connect(new InetSocketAddress(server, port));
			
			// read status line
			readAndVerifyStatus("220", commandSock);
			
			// log in with user and password.
			sendCommand("USER " + user + "\r\n", commandSock);
			
			// read user response
			readAndVerifyStatus("331", commandSock);
			
			// send password.
			sendCommand("PASS " + pass + "\r\n", commandSock);
			
			// read pass response
			readAndVerifyStatus("230", commandSock);
			
			// change directory to target path
			sendCommand("CWD " + path + "\r\n", commandSock);
			
			// read directory change successful.
			readAndVerifyStatus("250", commandSock);
			
			// send passive command
			sendCommand("PASV\r\n", commandSock);
			
			//227 Entering Passive Mode (192,0,32,8,39,237)
			String pasv = readAndVerifyStatus("227", commandSock).trim();
			Matcher match = PASV_PATTERN.matcher(pasv);
			if(!match.matches()){
				LOG.error("FTP cannot pull out ports for PASV command.");
				throw new ProtocolException("FTP cannot pull out ports for PASV command.");
			}
			String[] octets = match.group(1).split(",");
			int dataPort = Integer.valueOf(octets[4]) * 256 + Integer.valueOf(octets[5]);
			
			// request file
			sendCommand("RETR " + file + "\r\n", commandSock);
			
			dataSock.connect(new InetSocketAddress(server, dataPort));
			
			return new ByteArrayInputStream(consumeEntireChannel(dataSock));
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	protected void sendCommand(String cmd, SocketChannel out) throws IOException{
		LOG.debug("FTP sent: " + cmd.trim());
		ByteBuffer buf = ByteBuffer.allocate(cmd.length());
		buf.put(cmd.getBytes());
		buf.rewind();
		out.write(buf);
	}
	
	protected String readAndVerifyStatus(String status, SocketChannel in) throws IOException{
		ByteBuffer buf = ByteBuffer.allocate(255);
		in.read(buf);
		buf.rewind();
		String response = new String(buf.array(), "UTF-8");
		LOG.debug("FTP received: " + response.trim());
		if(!response.startsWith(status)){
			String code = response.substring(0, 3);
			LOG.error("FTP unexpected status code: " + code);
			throw new ProtocolException("Unexpected status code: " + code);
		}
		return response;
	}
	
	protected byte[] consumeEntireChannel(SocketChannel in) throws IOException{
		byte[] combined = new byte[0];
		ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);
		
		while(in.read(buf) > 0){
			combined = ArrayUtils.combine(combined, buf.array());
		}
		return combined;
	}
}
