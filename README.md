# jpurge
purge files containing a timestamp in their name, like "myfile-2017-06-02.log"

# Usage:

java -jar jpurge -c <config file> -m [dry|exec] -r

## Options
  c:  
      config file  
  m:  
      mode, dry mode just show what will be purged, exec mode do the purge  
  r:  
      recursive mode, follow directory's tree and purge files  
      
      
## Config file

The configuration file is java's properties file type.  
Values :  

  pattern : this is a regex containing the pattern to filter file and extract the timestamp  
  dateformet : this the format we'll use to transform the extracted pattern into java's Date object  
  purge : this the number of days to keep, ie files older than this value (extrated from pattern) will be purged  
  directory : the directory to purge  
    
  Note : character \ should be doubled in the regex and the directory  
    
### Example :  
  
pattern=.+(20\\d\\d-\\d\\d-\\d\\d).*  
dateformat=yyyy-MM-dd  
purge=35  
directory=C:\\test_fl\\logs  

=> will purge all files older than 35 days containing a date like "20xx-mm-dd" in their name in the directory c:\test_fl\logs  
