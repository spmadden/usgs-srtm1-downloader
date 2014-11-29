usgs-srtm1-downloader
=====================

Usage Instructions for USGS SRTM1 Downloader:
	java {options} -jar usgs-srtm1-downloader.jar

Options:
	 -Dusername={USGS EarthExplorer Username}
	 -Dpassword={USGS EarthExplorer Password}
	 -DminLatitude={Minimum Latitude to Download [inclusive] Decimal Degrees WGS84}
	 -DminLongitude={Minimum Longitude to Download [inclusive] Decimal Degrees WGS84}
	 -DmaxLatitude={Maximum Latitude to Download [inclusive] Decimal Degrees WGS84}
	 -DmaxLongitude={Maximum Longitude to Download [inclusive] Decimal Degrees WGS84}
	 -DnumThreads={Number of Download Threads to use [1, MAX_INT].}

Note:  You will need a USGS Earth Explorer Account.  Register at earthexplorer.usgs.gov