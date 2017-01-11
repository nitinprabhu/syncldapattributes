package com.southdevon.trust.ldapconnector;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.log4j.Logger;

import com.southdevon.trust.util.LdapUtil;
import com.southdevon.trust.util.PropertyUtil;

public class LDAPConnector {
	
	
	private static final Logger logger = Logger.getLogger(LDAPConnector.class);
	
	public static void main(String[] args) 
	{
		
		//get edirectory connection for identity vault
		LdapConnection eDirectoryLdapConnection=LdapUtil.getLDAPConnectionForIdentityVault();
		
		//Retrieve records from identity vault
		Map<String, String> eDirectoryRecords=retrieveUserRecordsFromIdVault(eDirectoryLdapConnection);
		
		//get ldap connection for ucs
		LdapConnection ucsLdapConnection=LdapUtil.getLDAPConnectionForUCSLdap();
		
		for (Map.Entry<String, String> entry : eDirectoryRecords.entrySet())
		{
		    String esrEmployeeNumber = entry.getValue();
		    		
		    String userName = entry.getKey();
		    
		    String userDN=retrieveDNForUCSUser(ucsLdapConnection,userName);
			
		    System.out.println("UserDN retrieved from UCS "+userDN+" for employee number "+esrEmployeeNumber);
		
		    if(userDN!=null)
		    {
		    	updateESREmployeeNumberForUCS(userDN,ucsLdapConnection,esrEmployeeNumber);
		    }
		    else
		    {
		    	System.out.println("UNABLE TO FIND USER WITH CN="+userName);
				String fileName="C:/Users/hippo/workspace/syncldapattributes/esrUsersAbsentFromAD.txt";
				writeToFile("cn="+userName,fileName);
		    }
			
		  
		}
		System.out.println("Completed processing of LDAP Records");
	}


	private static Map<String, String> retrieveUserRecordsFromIdVault(LdapConnection eDirectoryLdapConnection) 
	{
		Map<String, String> eDirectoryData=new HashMap<String, String>();
		 
		Properties identityVaultProperties=PropertyUtil.readPropertiesFile("C:/Users/hippo/workspace/syncldapattributes/src/main/resources/identityvaultldap.properties");
		
		String baseDN=identityVaultProperties.getProperty("ldap.basedn");
		
		EntryCursor cursor;
		try 
		{
			cursor = eDirectoryLdapConnection.search( baseDN, "(objectclass=inetOrgPerson)", SearchScope.ONELEVEL );
			while (cursor.next())
			{
			    try 
			    {
					Entry entry = cursor.get();
					
					if(entry.get("cn")!=null && entry.get("nhsivEmployeeNumber")!=null)
					{
						String userName= entry.get("cn").get().getString();
						String esrEmployeeNumber= entry.get("nhsivEmployeeNumber").get().getString();
						
						System.out.println("Username :"+userName +"ESR Employee Number :"+esrEmployeeNumber);
						eDirectoryData.put(userName, esrEmployeeNumber);
					}
					else
					{
						String fileName="C:/Users/hippo/workspace/syncldapattributes/dnListForEmptyESREmployeeNum.txt";
						writeToFile(entry.getDn().getName(),fileName);
					}
					
				} 
			    catch (CursorException e) 
			    {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			   
			}

			cursor.close();
		} 
		catch (LdapException e1) 
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
		catch (CursorException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		return eDirectoryData;
	}


	private static void updateESREmployeeNumberForUCS(String userDN, LdapConnection ucsLdapConnection,String esrEmployeeNumber) 
	{
		Modification esrEmpNo = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, "employeeNumber",esrEmployeeNumber);

		try 
		{
			System.out.println("Successfully Added ESR Employee number to user "+userDN);
			ucsLdapConnection.modify( userDN, esrEmpNo );
		} 
		catch (LdapException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private static String retrieveDNForUCSUser(LdapConnection ucsLdapConnection,String userName) 
	{
		String userDN=null;
	
		  try 
			{
				// Create the SearchRequest object
				SearchRequest req = new SearchRequestImpl();
				req.setScope( SearchScope.SUBTREE );
				req.addAttributes("dn");
				req.setTimeLimit(0);
				req.setFilter( "(uid="+userName+")" );
				
				Properties ucsProperties=PropertyUtil.readPropertiesFile("C:/Users/hippo/workspace/syncldapattributes/src/main/resources/ucsldap.properties");
				
				req.setBase( new Dn( ucsProperties.getProperty("ldap.basedn") ) );
				
				// Process the request
				SearchCursor searchCursor = ucsLdapConnection.search( req );
			
				while ( searchCursor.next() )
				{
					Response response = searchCursor.get();

					// process the SearchResultEntry
					if ( response instanceof SearchResultEntry )
					{
						Entry resultEntry = ( ( SearchResultEntry ) response ).getEntry();
						      
						userDN=resultEntry.getDn().getName();
					}
					
				}
		
		  } 
		  catch (LdapInvalidDnException ldapInvalidDnException)
		  {
				// TODO Auto-generated catch block
				ldapInvalidDnException.printStackTrace();
		  } 
		  catch (LdapException ldapException) 
		  {
			 ldapException.printStackTrace();
		  } 
		  catch (CursorException cursorException) 
		  {
				// TODO Auto-generated catch block
				cursorException.printStackTrace();
		  }
		return userDN;
	}
	
	public static void writeToFile(String data,String fileName)
	{
		File rejectedDNList = new File(fileName);
		FileWriter fileWriter;
		try 
		{
			fileWriter = new FileWriter(rejectedDNList, true);
			fileWriter.write(data+"\n");
			fileWriter.close();
		} 
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}

}
