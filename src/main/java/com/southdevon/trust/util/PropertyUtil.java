package com.southdevon.trust.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertyUtil {

	public static Properties readPropertiesFile(String fileName) 
	{
		Properties properties = new Properties();
		
		try 
		{
			properties.load(new FileInputStream(fileName));
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			

		return properties;
	}

}
