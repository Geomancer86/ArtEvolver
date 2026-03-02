@echo off
setlocal
echo.
echo  Downloading real sample images (Wikimedia, Picsum)...
echo  Replaces gradient placeholders with actual art and photos.
echo.
cd artevolver-core
call mvn -q compile exec:java -Dexec.mainClass="com.rndmodgames.evolver.clicker.DownloadSampleImages"
cd ..
echo.
echo  Done. Restart the clicker to see the updated images.
echo.
pause
