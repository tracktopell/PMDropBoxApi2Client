package com.tracktopell.dropboxclient;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;

import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderBuilder;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.WriteMode;
import com.dropbox.core.v2.users.FullAccount;
import java.io.BufferedReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Properties;
import org.apache.commons.codec.digest.DigestUtils;

public class Main {

	private static final String VERSION = "1.0.0";
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmssSSS");
	private static long deltaDifForUpload = 10;
	private static final String accessToken = "5kCy-CaDYpAAAAAAAAEiyl4mCLO75WU9f4_44nPEOO_qNmiepyfHcRLseb_4wBsm"; // dropbox@perfumeriamarlen.com.mx / pmarlen#Dr0pb0x
	private static boolean DEBUG = true;
	private static boolean TEST = false;
	private static final String CONTROLSYNC_FILE = "PMDB_ControlSync.data";
	private static final String CONTROLSYNC_PROP = "PMDB_ControlSync.properties";
	public static void main(String[] args) throws IOException, DbxException {
		
		String configRootPath  = "./";
		File   configRootFile = new File(configRootPath);
				
		if (args.length == 0) {
			System.err.println("==================================[PMDBx-Client " + VERSION + "]=================================");
			System.err.println("  usage: \tMain   -accessToken=DopBoxAccessToken -wp=workingRootPath   -lp=localRootPath   -debug=[true|false] -test=[true|false]");
			System.err.println("example server: \tMain   -accessToken=5kCy-CaDYpAAAAAAAAASgvkDbDCQ_r3SWrpasvDKnX4GY-Nm17oSiGtU0D2B3R2K -wp=/test_client   -lp=./PMDBx-Client/");
			System.err.println("example sucurs: \tMain   -accessToken=6rvKoYjb1tAAAAAAAAAAISaRz3GiZEWDwcO9uv6LoaB_s_AWv0u0DzQjGi06NNhH -wp=/def_png       -lp=./PMDBx-Client/");
			System.err.println("example sucurs: \tMain   -accessToken=6rvKoYjb1tAAAAAAAAAAISaRz3GiZEWDwcO9uv6LoaB_s_AWv0u0DzQjGi06NNhH -wp=/med_png       -lp=./PMDBx-Client/");
			System.exit(1);
		}
		
		String workingRootPath = null;
		String localRootPath   = null;
		String paramAccessToken = null;		
		
		for (String arg : args) {

			String[] av = arg.split("=");

			if (av.length == 2 && av[0].equals("-wp")) {
				workingRootPath = av[1];
				if (workingRootPath.equals("/")) {
					workingRootPath = "";
				}
			}
			if (av.length == 2 && av[0].equals("-lp")) {
				localRootPath = av[1];
			}
			if (av.length == 2 && av[0].equals("-debug")) {
				DEBUG = av[1].equalsIgnoreCase("true");
			}
			if (av.length == 2 && av[0].equals("-test")) {
				TEST = av[1].equalsIgnoreCase("true");
			}
			
			if (av.length == 2 && av[0].equals("-accessToken")) {
				paramAccessToken = av[1];
			}
		}
		if (paramAccessToken == null) {
			paramAccessToken = accessToken;
		}

		if (workingRootPath == null || localRootPath == null) {
			System.out.println("ARGUMENTS ERROR: must specify: -wp=workingRootPath   -lp=localRootPath");
			System.exit(1);
		}
		
		Properties propertiesX = new Properties();
		File propertiesFile = null;

		propertiesFile = new File(configRootFile,CONTROLSYNC_PROP);
		Date lastUpdate = null;
		Date now = new Date();
		long diffSync = 0;
		int  numSync  = 0;

		DbxRequestConfig config = new DbxRequestConfig("dropbox/java-tutorial");
		DbxClientV2 client = new DbxClientV2(config, paramAccessToken);
		FullAccount account = client.users().getCurrentAccount();

		if (propertiesFile.exists() && propertiesFile.canRead()) {
			try {
				System.out.println("OK, Properties (" + propertiesFile + ") Exist, then reading");
				propertiesX.load(new FileInputStream(propertiesFile));
				lastUpdate = sdf.parse(propertiesX.getProperty("LAST_UPDATE"));
				numSync    = Integer.parseInt(propertiesX.getProperty("NUM_SYNC"));
			} catch (Exception ioe) {
				System.err.println("Properties Exist but Can't read LAST_UPDATE from:" + propertiesFile);
				ioe.printStackTrace(System.err);
				System.exit(2);
			}
		} else {
			lastUpdate = now;
		}
				
		long mem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		
		diffSync = now.getTime() - lastUpdate.getTime();
		System.out.println("===============================[PMDropBoxApi2Client " + VERSION + "]==============================");
		System.out.println("\t   TOTAL_MEMORY : " + Runtime.getRuntime().totalMemory());
		System.out.println("\t    USED_MEMORY : " + mem);
		System.out.println("\t    FREE_MEMORY : " + Runtime.getRuntime().freeMemory());		
		System.out.println("");
		System.out.println("\t configRootPath : " + configRootFile.getAbsolutePath());
		System.out.println("\tworkingRootPath : " + (workingRootPath.equals("") ? "/" : workingRootPath));
		System.out.println("\t  localRootPath : " + localRootPath);
		System.out.println("\t            now : " + sdf.format(now));
		System.out.println("\t        Account : '" + account.getName().getDisplayName()+"', email='"+account.getEmail()+"', type="+account.getAccountType());
		System.out.println("\t     lastUpdate : " + sdf.format(lastUpdate) + ", from now is:" + prettyTime(diffSync));
		System.out.println("\t        NumSync : " + numSync);
		System.out.println("\t    AccessToken : " + paramAccessToken);
		System.out.println("=================================================================================================");
		
		File fileSync = new File(configRootFile, CONTROLSYNC_FILE);
		LinkedHashMap<String,SyncControlRecord> syncControlRecordMap = new LinkedHashMap<String,SyncControlRecord>();		
		
		if(fileSync.exists() && fileSync.isFile() && fileSync.canRead()){
			try{
				BufferedReader brSync= new BufferedReader(new InputStreamReader(new FileInputStream(fileSync)));
				String line =null;
				System.out.println("Reading SyncControlRecord : {");
				for(int numLines = 0; (line = brSync.readLine()) != null; numLines++){
					SyncControlRecord rr =  new SyncControlRecord(line);
					syncControlRecordMap.put(rr.getPath(), rr);
					System.out.println("\t=>"+rr);
				}
				System.out.println("}");
			}catch(IOException ioe){
				ioe.printStackTrace(System.err);
			}
		}
		
		File localRootPathDir = new File(localRootPath);
		System.out.println("Listing current Fiels in:"+localRootPathDir+" {");
		
		LinkedHashMap<String,LocalFileView> localFileViewMap = new LinkedHashMap<String,LocalFileView>();
		appendToListDir(localFileViewMap, localRootPathDir,localRootPathDir);
		for( LocalFileView lfv:localFileViewMap.values()){
			System.out.println("\tLocalFileView:"+lfv);
		}
		System.out.println("}");
		
		ListFolderBuilder listFolderBuilder = client.files().listFolderBuilder(workingRootPath);
		listFolderBuilder.withRecursive(true).withIncludeHasExplicitSharedMembers(true).withIncludeMediaInfo(true);
		ListFolderResult result = listFolderBuilder.start();

		File localFile = null;
		int countFiles = 0;
		
		LinkedHashMap<String,DropBoxExplicitMetadata> metadataMap = new LinkedHashMap<String,DropBoxExplicitMetadata>();
		FolderMetadata lastDmd = null; 
		for (Metadata md : result.getEntries()) {
			if (md instanceof FolderMetadata) {
				FolderMetadata dmd = (FolderMetadata) md;
				final DropBoxExplicitFolderMetadata demd = new DropBoxExplicitFolderMetadata(dmd);
				final LocalFileView ld = localFileViewMap.get(demd.getFolder().getPathLower());
				if(ld != null){
					ld.setExistInCloud(true);
				}
				metadataMap.put(dmd.getId(), demd);
				if(lastDmd != null && demd.getFolder().getPathLower().startsWith(lastDmd.getPathLower())){
					demd.setPid(lastDmd.getId());
					lastDmd = dmd;
				} else {
					lastDmd = dmd;
				}
				
			} else if (md instanceof FileMetadata) {
				FileMetadata   fmd = (FileMetadata) md;
				final DropBoxExplicitFileMetadata femd = new DropBoxExplicitFileMetadata(fmd);
				final LocalFileView ld = localFileViewMap.get(femd.getFile().getPathLower());
				if(ld != null){
					ld.setExistInCloud(true);
				}
				metadataMap.put(fmd.getId(), femd);
				if(lastDmd != null && femd.getFile().getPathLower().startsWith(lastDmd.getPathLower())){
					femd.setPid(lastDmd.getId());					
				} else {
					
				}
			}			
			countFiles++;
		}
		System.out.println("\tcountFiles=" + countFiles);

		localFile = null;
		long t1,t2;
		String md5 = null;
		
		for (DropBoxExplicitMetadata emd : metadataMap.values()) {
			FolderMetadata dmd = null;
			FileMetadata   fmd = null;
			
			if (emd.isDirectpory()) {
				dmd = emd.getFolder();
				System.out.print("{" + dmd.getId() + "} " + dmd.getPathLower());

				localFile = new File(localRootPath, dmd.getPathLower());
				if (!localFile.exists()) {
					boolean created = localFile.mkdirs();
					System.out.println("[m]");
				} else {
					System.out.println("[ ]");
				}
			} else {
				fmd = emd.getFile();

				System.out.print("[" + fmd.getId() + "] " + fmd.getPathLower() + "\tsize:" + fmd.getSize());

				localFile = new File(localRootPath, fmd.getPathLower());

				if (!localFile.exists()) {
					DbxDownloader<FileMetadata> download = client.files().download(fmd.getPathLower());
					FileOutputStream fos = new FileOutputStream(localFile);
					download.download(fos);
					fos.close();
					
					t1 = System.currentTimeMillis();
					md5 = calculateMD5(localFile);
					emd.setMd5(md5);
					t2 = System.currentTimeMillis();
					System.out.println("[v] ("+md5+ ") [cm:" + sdf.format(fmd.getClientModified()) + ",sm:" + sdf.format(fmd.getServerModified())+"] <"+(t2-t1)+">");
				} else {
					long lfm = localFile.lastModified();
					long rfm = fmd.getServerModified().getTime();
					long cfm = fmd.getClientModified().getTime();
					
					boolean shouldDownload = (rfm - lfm) >0;
					boolean shouldUpload   = (rfm - lfm)<=0;
					if(shouldDownload){
						DbxDownloader<FileMetadata> download = client.files().download(fmd.getPathLower());
						FileOutputStream fos = new FileOutputStream(localFile);
						download.download(fos);
						fos.close();
						t1 = System.currentTimeMillis();
						md5 = calculateMD5(localFile);
						t2 = System.currentTimeMillis();
						emd.setMd5(md5);
						System.out.println("[V] :: " + localFile.length() + " :: ("+md5+") [lm:"+sdf.format(new Date(lfm))+", sm:"+sdf.format(new Date(rfm))+", cm:"+sdf.format(new Date(cfm))+"] "+(shouldDownload?"V":" ")+"<"+(t2-t1)+">");
					} else if(shouldUpload){
						t1 = System.currentTimeMillis();
						md5 = calculateMD5(localFile);
						t2 = System.currentTimeMillis();
						
						SyncControlRecord xt6 = syncControlRecordMap.get(fmd.getPathLower());
						if(xt6!=null && !xt6.getMd5().equals(md5)){
							emd.setMd5(md5);						
							FileInputStream fis = new FileInputStream(localFile);
							client.files().uploadBuilder(fmd.getPathLower()).
									withMode(WriteMode.OVERWRITE).
									withClientModified(new Date(lfm)).
									uploadAndFinish(fis);
							System.out.println("[^] :: " + localFile.length() + " :: ("+md5+") [lm:"+sdf.format(new Date(lfm))+", sm:"+sdf.format(new Date(rfm))+", cm:"+sdf.format(new Date(cfm))+"] "+(shouldDownload?"U":" ")+"<"+(t2-t1)+">");
						} else {
							emd.setMd5(md5);
							System.out.println("[=] :: " + localFile.length() + " :: ("+md5+") [lm:"+sdf.format(new Date(lfm))+", sm:"+sdf.format(new Date(rfm))+", cm:"+sdf.format(new Date(cfm))+"] =<"+(t2-t1)+">");
						}											
					} else {
						t1 = System.currentTimeMillis();
						md5 = calculateMD5(localFile);
						t2 = System.currentTimeMillis();
						emd.setMd5(md5);
						System.out.println("[_] :: " + localFile.length() + " :: ("+md5+") [lm:"+sdf.format(new Date(lfm))+", sm:"+sdf.format(new Date(rfm))+", cm:"+sdf.format(new Date(cfm))+"] =<"+(t2-t1)+">");
					}					
				}
			}
		}
		if(numSync>0){
			System.out.println("syncControlRecordMap:After Downlad{");
			for(String pf:syncControlRecordMap.keySet()){
				System.out.println("\tX before?"+pf);
			}
			System.out.println("}");
			System.out.println("After Downlad{");
			for( LocalFileView lfv:localFileViewMap.values()){
				SyncControlRecord rx9 = syncControlRecordMap.get(lfv.getPath());
				boolean existBefore = (rx9 != null);				
				
				System.out.print("\tLocalFileView:"+lfv+"\t"+(existBefore?" ":"ud?"));
				if(!existBefore){
					File fileToDelete = new File(localRootPath, lfv.getPath());
					if(fileToDelete.isDirectory()){
						fileToDelete.delete();
						System.out.println("\trm dir ?");
					} else {
						fileToDelete.delete();
						System.out.println("\trm OK");
					}
					
				} else {
					System.out.println("\t-");
				}
			}
			System.out.println("}");
		}
		
		
		
		PrintStream psProprs = new PrintStream(new File(configRootFile, CONTROLSYNC_FILE));
		for (DropBoxExplicitMetadata emd : metadataMap.values()) {
			psProprs.println(emd.toString());
		}
		
		propertiesX.setProperty("LAST_UPDATE",sdf.format(new Date()));
		propertiesX.setProperty("NUM_SYNC",String.valueOf(++numSync));
		
		propertiesX.store(new FileOutputStream(propertiesFile), "Update ant end");
	}

	private static MessageDigest md = null;

	private static byte buffer[] = null;
	
	private static String calculateMD5(File file) throws IOException{
		return DigestUtils.md5Hex(new FileInputStream(file));
	}
	
	private static String _calculateMD5(File file) {
		if (md == null) {
			try {
				md = MessageDigest.getInstance("MD5");
			}catch(NoSuchAlgorithmException nsae){
				throw new IllegalStateException("MD5 cant't calculate:"+nsae.getMessage());
			}
		} else {
			md.reset();
		}
		InputStream       fis = null;
		try {
			fis = new FileInputStream(file);
		}catch(IOException ioe){
			throw new IllegalArgumentException("File cna't open:"+file);
		}
		DigestInputStream dis = new DigestInputStream(fis, md);
		if(buffer == null){
			buffer = new byte[1048576]; // 1MB
		}
		
		int numRead;
		try{
			do {
				numRead = dis.read(buffer);
				if (numRead > 0) {
					md.update(buffer, 0, numRead);
				}
			} while (numRead != -1);

			dis.close();
		}catch(IOException ioe){
			throw new IllegalStateException("MD5 cant't calculate:"+ioe.getMessage());
		}
		byte[] digest = md.digest();

		String result = "";

		for (int i = 0; i < digest.length; i++) {
			result += Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1);
		}
		return result;

	}

	private static String prettyTime(long msR) {
		long ms = Math.abs(msR);
		long x, milliseconds, seconds, minutes, hours, days;
		milliseconds = ms % 1000;
		x = ms / 1000;
		seconds = x % 60;
		x /= 60;
		minutes = x % 60;
		x /= 60;
		hours = x % 24;
		x /= 24;
		days = x;

		StringBuffer sb = new StringBuffer();

		if (days > 0) {
			sb.append(days);
			sb.append(" D. ");
		}
		if (hours > 0) {
			sb.append(hours);
			sb.append(" H. ");
		}
		if (minutes > 0) {
			sb.append(minutes);
			sb.append(" m. ");
		}
		if (seconds > 0) {
			sb.append(seconds);
			sb.append(" s.");
		}
		if (milliseconds > 0) {
			sb.append(milliseconds);
			sb.append(" ms.");
		}

		if (msR == 0) {
			sb.append("[SAME TIME]");
		}

		return sb.toString();
	}
	
	private static void appendToListDir(LinkedHashMap<String,LocalFileView> listDist,File dir,File root){
		File[] listFiles = dir.listFiles();
		if(listFiles != null){
			for(File f: listFiles){
				if(f.isDirectory()){
					String name = f.getPath().replace(root.getName(), "").replace("\\", "/");
					listDist.put(name,new LocalFileView("d", name, null, false));
					appendToListDir(listDist, f,root);
				} else{
					String name = f.getPath().replace(root.getName(), "").replace("\\", "/");
					listDist.put(name,new LocalFileView("f", name, null, false));
				}
			}
		}
	}
}

class LocalFileView{
	private String type;
	private String path;
	private String md5;	
	private boolean existInCloud;

	public LocalFileView(String type, String path, String md5, boolean existInCloud) {
		this.type = type;
		this.path = path;
		this.md5 = md5;
		this.existInCloud = false;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * @return the md5
	 */
	public String getMd5() {
		return md5;
	}

	/**
	 * @param md5 the md5 to set
	 */
	public void setMd5(String md5) {
		this.md5 = md5;
	}

	/**
	 * @return the existInCloud
	 */
	public boolean isExistInCloud() {
		return existInCloud;
	}

	/**
	 * @param existInCloud the existInCloud to set
	 */
	public void setExistInCloud(boolean existInCloud) {
		this.existInCloud = existInCloud;
	}

	@Override
	public String toString() {
		return type+"|"+path+"|"+(existInCloud?"*":"_");
	}
	
}

class SyncControlRecord {
	private String t;
	private String pid;
	private String id;
	private String path;
	private Long   size;
	private String md5;

	public SyncControlRecord(String line) {
		String r[]		= line.split("\\|");
		final String er = "SyncControlRecord line=\""+line+"\"\t, split="+Arrays.asList(r);
		System.err.println(er);
		
		this.t			= r[0];
		if(r[1] != null && r[1].length()>0){
			this.pid		= r[1];
		}
		this.id			= r[2];
		this.path		= r[3];
		
		if(r.length> 4 ){
			if(r[4] != null && r[4].length()>0){
				this.size	= new Long(r[4]);
			}
			if(r[5] != null&& r[5].length()>0){
				this.md5	= r[5];
			}
		}
	}

	public SyncControlRecord(String t, String pid, String id, String path, Long size, String md5) {
		this.t = t;
		this.pid = pid;
		this.id = id;
		this.path = path;
		this.size = size;
		this.md5 = md5;
	}
	

	/**
	 * @return the t
	 */
	public String getT() {
		return t;
	}

	/**
	 * @param t the t to set
	 */
	public void setT(String t) {
		this.t = t;
	}

	/**
	 * @return the pid
	 */
	public String getPid() {
		return pid;
	}

	/**
	 * @param pid the pid to set
	 */
	public void setPid(String pid) {
		this.pid = pid;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * @return the size
	 */
	public Long getSize() {
		return size;
	}

	/**
	 * @param size the size to set
	 */
	public void setSize(Long size) {
		this.size = size;
	}

	/**
	 * @return the md5
	 */
	public String getMd5() {
		return md5;
	}

	/**
	 * @param md5 the md5 to set
	 */
	public void setMd5(String md5) {
		this.md5 = md5;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(t);
		sb.append("|");
		if(pid != null){
			sb.append(pid);
		}else {
			sb.append("");
		}
		sb.append("|");
		sb.append(id);
		sb.append("|");
		sb.append(path);
		sb.append("|");
		if(size != null){
			sb.append(size);
		} else{
			sb.append("");
		}
		sb.append("|");
		if(md5!= null){
			sb.append(md5);
		} else {
			sb.append("");
		}
		
		return sb.toString();

	}
	
	
}

interface DropBoxExplicitMetadata{
	boolean isDirectpory();
	String getId();
	String getPid();
	void setPid(String pid);
	FolderMetadata getFolder();
	FileMetadata   getFile();
	void setMd5(String  md5);
	String getMd5();
}

class DropBoxExplicitFolderMetadata implements DropBoxExplicitMetadata{
	private FolderMetadata dmd;
	private String md5;
	private String pid;
	
	public DropBoxExplicitFolderMetadata(Metadata md){
		dmd = (FolderMetadata)md;
	}

	@Override
	public String getId() {
		return dmd.getId();
	}
	
	@Override
	public boolean isDirectpory() {
		return true;
	}

	@Override
	public FolderMetadata getFolder() {
		return dmd;
	}

	@Override
	public FileMetadata getFile() {
		throw new IllegalStateException("Wrong code invocation");
	}

	public void setMd5(String md5) {
		this.md5 = md5;
	}

	@Override
	public String getMd5() {
		return md5;
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("d|");
		if(getPid() != null){
			sb.append(getPid());
		}else {
			sb.append("");
		}
		sb.append("|");
		sb.append(dmd.getId());
		sb.append("|");
		sb.append(dmd.getPathLower());
		sb.append("||");
		return sb.toString();
	}

	@Override
	public String getPid() {
		return this.pid;
	}
	
	@Override
	public void setPid(String pid) {
		this.pid = pid;
	}
	
	@Override
	public boolean equals(Object obj) {
		return this.getId().equals(((DropBoxExplicitMetadata)obj).getId());
	}
	
}

class DropBoxExplicitFileMetadata implements DropBoxExplicitMetadata{
	private FileMetadata fmd;
	private String md5;
	private String pid;
	
	public DropBoxExplicitFileMetadata(Metadata md){
		fmd = (FileMetadata)md;
	}
	
	@Override
	public String getId() {
		return fmd.getId();
	}
	
	@Override
	public boolean isDirectpory() {
		return false;
	}

	@Override
	public FolderMetadata getFolder() {
		throw new IllegalStateException("Wrong code invocation");
	}

	@Override
	public FileMetadata getFile() {
		return fmd;
	}
	
	public void setMd5(String md5) {
		this.md5 = md5;
	}

	@Override
	public String getMd5() {
		return md5;
	}
	
	@Override
	public String getPid() {
		return this.pid;
	}
	
	@Override
	public void setPid(String pid) {
		this.pid = pid;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("-|");
		if(getPid() != null){
			sb.append(getPid());
		}else {
			sb.append("");
		}
		sb.append("|");
		sb.append(fmd.getId());
		sb.append("|");
		sb.append(fmd.getPathLower());
		sb.append("|");
		sb.append(fmd.getSize());
		sb.append("|");
		if(getMd5()!= null){
			sb.append(getMd5());
		} else {
			sb.append("");
		}
		return sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		return this.getId().equals(((DropBoxExplicitMetadata)obj).getId());
	}
}