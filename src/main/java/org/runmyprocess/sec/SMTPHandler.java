package org.runmyprocess.sec;


import org.runmyprocess.sec.Config;
import org.runmyprocess.sec.GenericHandler;
import org.runmyprocess.sec.SECErrorManager;

import java.io.File;
import java.util.logging.Level;


public class SMTPHandler {

    public static void main(String [] args)throws java.io.IOException{
    	
		try{
			GenericHandler genericHandler = new	GenericHandler();
			System.out.println("Searching for config file...");
			Config conf = new Config("configFiles"+File.separator+"handler.config",true);
			System.out.println("Handler config file found for manager ping port "+
								conf.getProperty("managerPort"));
			genericHandler.run(conf);
    	}catch( Exception e ){
	        	SECErrorManager errorManager = new SECErrorManager();
	        	errorManager.logError(e.getMessage(), Level.SEVERE);
	        	e.printStackTrace();
    		}
        }

}
