
package com.tracktopell.dropboxclient;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;

import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DbxUserFilesRequests;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderBuilder;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.users.FullAccount;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class Main {
	private static final String VERSION = "1.0.0";
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd_HH:mm:ss.SSS");
	private static long deltaDifForUpload   = 10;
	private static final String accessToken = "5kCy-CaDYpAAAAAAAAEiyl4mCLO75WU9f4_44nPEOO_qNmiepyfHcRLseb_4wBsm"; // dropbox@perfumeriamarlen.com.mx / pmarlen#Dr0pb0x
	private static boolean  DEBUG = true;
	private static boolean  TEST  = false;
    public static void main(String[] args) throws IOException, DbxException {
		if(args.length == 0){
			System.err.println("==================================[PMDBx-Client "+VERSION+"]=================================");			
			System.err.println("  usage: \tMain   -accessToken=DopBoxAccessToken -wp=workingRootPath   -lp=localRootPath   -debug=[true|false] -test=[true|false] -propFile=propFilePath");
			System.err.println("example server: \tMain   -accessToken=5kCy-CaDYpAAAAAAAAASgvkDbDCQ_r3SWrpasvDKnX4GY-Nm17oSiGtU0D2B3R2K -wp=/test_client   -lp=./PMDBx-Client/");
			System.err.println("example sucurs: \tMain   -accessToken=6rvKoYjb1tAAAAAAAAAAISaRz3GiZEWDwcO9uv6LoaB_s_AWv0u0DzQjGi06NNhH -wp=/def_png       -lp=./PMDBx-Client/");
			System.err.println("example sucurs: \tMain   -accessToken=6rvKoYjb1tAAAAAAAAAAISaRz3GiZEWDwcO9uv6LoaB_s_AWv0u0DzQjGi06NNhH -wp=/med_png       -lp=./PMDBx-Client/");
			System.exit(1);
		}
		
		String workingRootPath = null;
		String localRootPath   = null;
		String propFilePath    = null;
		String paramAccessToken= null;
		
		for(String arg:args){
			
			String[] av = arg.split("=");
			
			if(av.length==2 && av[0].equals("-wp")){
				workingRootPath = av[1];
				if(workingRootPath.equals("/")){
					workingRootPath = "";
				}
			}
			if(av.length==2 && av[0].equals("-lp")){
				localRootPath = av[1];
			}
			if(av.length==2 && av[0].equals("-debug")){
				DEBUG = av[1].equalsIgnoreCase("true");
			}
			if(av.length==2 && av[0].equals("-test")){
				TEST = av[1].equalsIgnoreCase("true");
			}
			if(av.length==2 && av[0].equals("-propFile")){
				propFilePath = av[1];
			}
			if(av.length==2 && av[0].equals("-accessToken")){
				paramAccessToken = av[1];
			}
		}
		if(paramAccessToken==null){
			paramAccessToken =  accessToken;
		}
		
		if(workingRootPath == null || localRootPath==null){
			System.out.println("ARGUMENTS ERROR: must specify: -wp=workingRootPath   -lp=localRootPath");
			System.exit(1);
		}
		
		if(propFilePath == null){
			propFilePath = "../config/.PMDBx-Client.properties";
		}

		
		Properties properties=new Properties();
		File propertiesFile=null;
		
		propertiesFile  = new File(propFilePath);
		Date lastUpdate = null;
		Date now		= new Date();
		long diffSync   = 0;
		
       	DbxRequestConfig config = new DbxRequestConfig("dropbox/java-tutorial");
		DbxClientV2 client = new DbxClientV2(config, paramAccessToken );
		FullAccount account = client.users().getCurrentAccount();

		if(propertiesFile.exists() && propertiesFile.canRead()){
			try {
				System.out.println("OK, Properties ("+propertiesFile+") Exist, then reading");
				properties.load(new FileInputStream(propertiesFile));
				lastUpdate = sdf.parse(properties.getProperty("LAST_UPDATE"));
			}catch(Exception ioe){
				System.err.println("Properties Exist but Can't read LAST_UPDATE from:"+propertiesFile);
				ioe.printStackTrace(System.err);
				System.exit(2);
			}
		} else {
			lastUpdate = now;
		}
		
		diffSync   = now.getTime() - lastUpdate.getTime();
		System.out.println("==================================[PMDBx-Client "+VERSION+"]=================================");
		System.out.println("\t\tworkingRootPath:"+(workingRootPath.equals("")?"/":workingRootPath));
		System.out.println("\t\t  localRootPath:"+localRootPath);
		System.out.println("\t\t            now:"+sdf.format(now));
		System.out.println("\t\t        Account:"+account.getName().getDisplayName());						
		System.out.println("\t\t     lastUpdate:"+sdf.format(lastUpdate)+", from now is:"+prettyTime(diffSync));
		System.out.println("\t\t    AccessToken:"+paramAccessToken);
		System.out.println("=============================================================================================");
		
		
		ListFolderBuilder listFolderBuilder = client.files().listFolderBuilder(workingRootPath);
		listFolderBuilder.withRecursive(true).withIncludeHasExplicitSharedMembers(true).withIncludeMediaInfo(true);		
		ListFolderResult result = listFolderBuilder.start();
		       
		File localFile = null;
		int countFiles = 0;
        for (Metadata md : result.getEntries()) {
			countFiles++;
		}
		System.out.println("\tcountFiles="+countFiles);
		
		listFolderBuilder = client.files().listFolderBuilder(workingRootPath);
		listFolderBuilder.withRecursive(true).withIncludeHasExplicitSharedMembers(true).withIncludeMediaInfo(true);		
		result = listFolderBuilder.start();
		
		localFile = null;
		
		for (Metadata md : result.getEntries()) {
			if(md instanceof FolderMetadata){
				FolderMetadata dmd = (FolderMetadata)md;
				System.out.print("\t{"+dmd.getId()+"} "+dmd.getPathLower());

				localFile = new File(localRootPath,dmd.getPathLower());
				if(!localFile.exists()){
					boolean created = localFile.mkdirs();
					System.out.println(" [v]");
				} else {
					System.out.println(" [-]");
				}
			} else if(md instanceof FileMetadata){
				FileMetadata   fmd = (FileMetadata)md;
				
				System.out.print("\t["+fmd.getId()+"] "+fmd.getPathLower()+"\tsize:"+fmd.getSize()+"\tcm:"+fmd.getClientModified()+"\tsm"+fmd.getServerModified());

				localFile = new File(localRootPath,fmd.getPathLower());

				if(!localFile.exists()){
					DbxDownloader<FileMetadata> download = client.files().download(fmd.getPathLower());
					FileOutputStream fos = new FileOutputStream(localFile);
					download.download(fos);
					fos.close();
					System.out.println(" [v]");
				} else {						
					System.out.println(" [-] :: "+localFile.length());
				}
			}
		}

		System.out.println("\t[3]->");		
		//listRecursevly(client, "", 0);		
    }
	
	
	private static String prettyTime(long msR){
		long ms = Math.abs(msR);
		long x,milliseconds,seconds,minutes,hours,days;
		milliseconds = ms % 1000;
		x = ms / 1000;
		seconds = x % 60;
		x /= 60;
		minutes = x % 60;
		x /= 60;
		hours = x % 24;
		x /= 24;
		days = x;
	
		StringBuffer sb=new StringBuffer();
		
		if(days>0){
			sb.append(days);
			sb.append(" D. ");
		}
		if(hours>0){
			sb.append(hours);
			sb.append(" H. ");
		}
		if(minutes>0){
			sb.append(minutes);
			sb.append(" m. ");
		}		
		if(seconds>0){
			sb.append(seconds);
			sb.append(" s.");
		}
		if(milliseconds>0){
			sb.append(milliseconds);
			sb.append(" ms.");
		}
		
		if(msR==0){
			sb.append("[SAME TIME]");
		}
		
		return sb.toString();
	}
}
