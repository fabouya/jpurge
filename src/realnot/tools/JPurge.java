package realnot.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

public class JPurge 
{
	private static boolean	_recurse = false;
	private static boolean	_drymode = true;
	private static String	_configFile = "";
	private static String  	_pattern = ""; 
	private static String  	_directory = ""; 
	private static long		_purge = 10000;
	private static Matcher 	_matcher;
	private static String	_dateFormat="";

	protected final static Logger _logger = Logger.getLogger(JPurge.class);	

	public static void main(String[] args) 
	{
		// TODO Auto-generated method stub
		
		DoParseOptions(args);
		ReadProp();
		if(_recurse)
		{
			DoRecursePurge(_directory);
		}
		else
		{
			DoPurge(_directory);
		}

		_logger.info("OK");

		System.exit(0);
	}

	private static void ReadProp()
	{

		Properties prop = new Properties();
		InputStream input = null;

		try 
		{
			input = new FileInputStream(_configFile);

			// load a properties file
			prop.load(input);

			// get the property value and print it out
			_pattern = prop.getProperty("pattern");
			_directory = prop.getProperty("directory");
			_purge = Integer.parseInt(prop.getProperty("purge"));
			_dateFormat = prop.getProperty("dateformat");
			
			_logger.info("Pattern : " + _pattern);
			_logger.info("Directory : " + _directory);
			_logger.info("Purge : fichiers plus anciens que " + _purge + " jours");
			_logger.info("Format Date : " + _dateFormat);

		} 
		catch (Exception ex) 
		{
			_logger.error(ex.getMessage());
			System.exit(1);
		} 
		finally 
		{
			if (input != null) 
			{
				try {
					input.close();
				} catch (IOException e) 
				{
					_logger.error(e.getMessage());
					System.exit(2);
				}
			}
		}
		
	}
	
	private static void DoParseOptions(String[] args)
	{
		// create Options object
		Options options = new Options();


		options.addOption("r", false, "Recurse");
		
		Option  Omode = Option.builder("m")
						.hasArg()
						.longOpt("mode")
						.required()
						.argName("mode")
						.build();
		
		options.addOption(Omode);

		Option  Oconf = Option.builder("c")
				.hasArg()
				.longOpt("config")
				.required()
				.argName("config")
				.build();

		options.addOption(Oconf);
		
		CommandLineParser parser = new DefaultParser();
		
		try 
		{
			CommandLine cmd = parser.parse( options, args);
						
			String mode = cmd.getOptionValue("mode");

			if(mode.toLowerCase().equals("exec"))
			{
				_logger.info("mode exec");
				_drymode = false;
			}
			else
				if(mode.toLowerCase().equals("dry"))
				{
					_logger.info("mode dry");
					_drymode = true;
				}
				else
				{
					_logger.error("mode s'execution inconnu : " + mode);
					System.exit(1);
				}
			
			_configFile = cmd.getOptionValue("config");
			_logger.info("Config : " + _configFile);
			
			
			if(cmd.hasOption('r'))
			{
				_recurse = true;
			}

			_logger.info("Recursif : " + (_recurse ? "oui" : "non"));
			
		} catch (ParseException e) 
		{
			// TODO Auto-generated catch block
			_logger.error(e.getMessage());
			System.exit(1);
		}				
		
	}
	
	private static void DoRecursePurge(String baseDirectory)
	{
		_logger.debug("Start recursive purge : " + baseDirectory);
		
		DoPurge(baseDirectory);

		File rep = new File(baseDirectory);
		
		if ( rep.isDirectory ( ) ) 
		{
            File[] list = rep.listFiles();
            
            if (list != null)
            {
                for ( int i = 0; i < list.length; i++) 
                {
                	_logger.debug("Checking for recurse: " + list[i].getName());
                	
            		if(list[i].isDirectory())
            		{
                    	_logger.debug(list[i].getName() + " est un directory");

            			DoRecursePurge(list[i].getPath());
            		}
                } 
            } 
		} 		
		
	}
	
	private static void DoPurge(String directory)
	{
		// parcourir les fichiers du répertoire (arborescence si recurse)
		// verifier s'ils match le pattern
		// supprimer si exec indiquer si dry
		
		_logger.debug("Start purge : " + directory);
		
		Pattern pattern = Pattern.compile(_pattern);
		
		File rep = new File(directory);
		
		File[] fichiers = rep.listFiles(new FilenameFilter() 
		{
		  public boolean accept(File dir, String name) 
		  {
			  _matcher = pattern.matcher(name);
			  _logger.debug("verification du nom : " + name);
		    return _matcher.find();
		  }
		});				
		
		SimpleDateFormat formatter = new SimpleDateFormat(_dateFormat);
		  
		for(int i = 0; i < fichiers.length; i++)
		{
			_logger.debug("fichier match : " + fichiers[i].getName());
			// Extraction de la date
			_matcher = pattern.matcher(fichiers[i].getName());
			
			Calendar c = Calendar.getInstance ();
			Date today = c.getTime ();
			
			while (_matcher.find()) 
			{ 
				  //System.out.println(_matcher.group(1)); 
				  
				  if(fichiers[i].isDirectory())
				  {
				  		_logger.debug(fichiers[i].getName() + " match mais c'est un répertoire");
				  		continue;  
				  }
				  
				  try 
				  {
					  	Date date = formatter.parse(_matcher.group(1));
					  	
					  	long diff = today.getTime() - date.getTime();
					  	long diffSeconds = diff / 1000;
					  	long diffjour = diffSeconds / 86400;
					  	
					  	if(diffjour >= _purge)
					  	{					  		
					  		_logger.info("A Purger " + fichiers[i].getName() + "(" + diffjour + " jours)");
					  		
					  		if(!_drymode)
					  		{
						  		// elligible à la purge et mode exec
					  				if(!fichiers[i].delete())
					  				{
						  				_logger.error("Erreur de suppression de " + fichiers[i].getName());
					  				}
					  		}
					  	}
				  } 
				  catch (java.lang.IndexOutOfBoundsException ei)
				  {
					  _logger.error(ei.getMessage());
					  _logger.error("Erreur lors de l'extraction de la date, il manque surement des parenthese autour du modele dans la regex :" + _pattern);
					  System.exit(3);					  
				  }
				  catch (java.text.ParseException e) 
				  {
						// TODO Auto-generated catch block
						_logger.error(e.getMessage());
						System.exit(3);					  
				  }
			}
		}
		
	}
	
}
