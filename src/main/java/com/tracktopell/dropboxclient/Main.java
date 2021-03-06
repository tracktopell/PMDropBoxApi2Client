package com.tracktopell.dropboxclient;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;

import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderBuilder;
import com.dropbox.core.v2.files.ListFolderGetLatestCursorResult;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.WriteMode;
import com.dropbox.core.v2.users.FullAccount;
import java.io.BufferedReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

public class Main {

	//private static final String VERSION = "1.0.0";
	public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmssSSS");
	private static long deltaDifForUpload = 10;
	private static final String accessToken = "5kCy-CaDYpAAAAAAAAEiyl4mCLO75WU9f4_44nPEOO_qNmiepyfHcRLseb_4wBsm"; // dropbox@perfumeriamarlen.com.mx / pmarlen#Dr0pb0x
	private static boolean DEBUG = true;
	private static boolean TEST = false;
	private static final String CONTROLSYNC_FILE = "PMDB_ControlSync.data";
	private static final String CONTROLSYNC_PROP = "PMDB_ControlSync.properties";

	private static String project_vesion = null;

	private static String getVersion() {
		if (project_vesion == null) {
			Properties propVersion = new Properties();
			try {
				propVersion.load(Main.class.getResourceAsStream("/Version.properties"));
				project_vesion = propVersion.getProperty("project.version");
			} catch (IOException ioe) {
				project_vesion = "-.-";
				ioe.printStackTrace(System.err);
			}
		}
		return project_vesion;

	}

	public static void main(String[] args) throws IOException, DbxException {
		printMemoryUsage();
		
		String configRootPath = "./";
		File configRootFile = new File(configRootPath);

		if (args.length == 0) {
			System.err.println("==================================[PMDBx-Client " + getVersion() + "]=================================");
			System.err.println("  usage: Main  -listFiles=true|false   -accessToken=DopBoxAccessToken_XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX -wp=workingRootPath   -lp=localRootPath   -debug=[true|false] -test=[true|false]");
			System.err.println("example server: Main -listFiles=false  -accessToken=5kCy-CaDYpAAAAAAAAEiyl4mCLO75WU9f4_44nPEOO_qNmiepyfHcRLseb_4wBsm -wp=/test_client   -lp=./PMDBx-Client/");
			System.exit(1);
		}
		boolean listFiles = false;
		String workingRootPath = null;
		String localRootPath = null;
		String paramAccessToken = null;

		for (String arg : args) {

			String[] av = arg.split("=");

			if (av.length == 2 && av[0].equals("-wp")) {
				workingRootPath = av[1];
				if (workingRootPath.equals("/")) {
					workingRootPath = "";
				}
			}
			if (av.length == 2 && av[0].equals("-listFiles")) {
				listFiles = av[1].equals("true");
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

		propertiesFile = new File(configRootFile, CONTROLSYNC_PROP);
		Date lastUpdate = null;
		Date now = new Date();
		long diffSync = 0;
		int numSync = 0;

		DbxRequestConfig config = new DbxRequestConfig("dropbox/java-tutorial");
		DbxClientV2 client = new DbxClientV2(config, paramAccessToken);
		FullAccount account = client.users().getCurrentAccount();

		if (propertiesFile.exists() && propertiesFile.canRead()) {
			try {
				System.out.println("OK, Properties (" + propertiesFile + ") Exist, then reading");
				propertiesX.load(new FileInputStream(propertiesFile));
				lastUpdate = sdf.parse(propertiesX.getProperty("LAST_UPDATE"));
				numSync = Integer.parseInt(propertiesX.getProperty("NUM_SYNC"));
			} catch (Exception ioe) {
				System.err.println("Properties Exist but Can't read LAST_UPDATE from:" + propertiesFile);
				ioe.printStackTrace(System.err);
				System.exit(2);
			}
		} else {
			lastUpdate = now;
		}

		

		diffSync = now.getTime() - lastUpdate.getTime();
		System.out.println("===============================[PMDropBoxApi2Client " + getVersion() + "]==============================");
		printMemoryUsage();
		System.out.println("");
		System.out.println("\t configRootPath : " + configRootFile.getAbsolutePath());
		System.out.println("\tworkingRootPath : " + (workingRootPath.equals("") ? "/" : workingRootPath));
		System.out.println("\t  localRootPath : " + localRootPath);
		System.out.println("\t            now : " + sdf.format(now));
		System.out.println("\t        Account : '" + account.getName().getDisplayName() + "', email='" + account.getEmail() + "', type=" + account.getAccountType());
		System.out.println("\t     lastUpdate : " + sdf.format(lastUpdate) + ", from now is:" + prettyTime(diffSync));
		System.out.println("\t        NumSync : " + numSync);
		System.out.println("\t    AccessToken : " + paramAccessToken);
		System.out.println("=================================================================================================");

		if(TEST){
			System.exit(0);
		}
		
		File fileSync = new File(configRootFile, CONTROLSYNC_FILE);
		LinkedHashMap<String, SyncControlRecord> syncBeforeControlRecordMap = new LinkedHashMap<String, SyncControlRecord>();

		if (fileSync.exists() && fileSync.isFile() && fileSync.canRead()) {
			try {
				BufferedReader brSync = new BufferedReader(new InputStreamReader(new FileInputStream(fileSync)));
				String line = null;
				System.out.println("Reading SyncControlRecord : {");
				for (int numLines = 0; (line = brSync.readLine()) != null; numLines++) {
					SyncControlRecord rr = new SyncControlRecord(line);
					syncBeforeControlRecordMap.put(rr.getPath(), rr);					
				}
				System.out.println("}");
			} catch (IOException ioe) {
				ioe.printStackTrace(System.err);
			}
		}

		File localRootPathDir = new File(localRootPath);
		LinkedHashMap<String, LocalFileView> localFileViewMap = new LinkedHashMap<String, LocalFileView>();
		if (!listFiles) {
			System.out.println("Searching Local Fiels for (local,path):(" + localRootPathDir + "/" + workingRootPath + ") {");
			appendToListDir(localFileViewMap, localRootPathDir, localRootPathDir, workingRootPath);
			System.out.println("}");
		}

		ListFolderResult result = null;

		File localFile = null;
		int countFiles = 0;
		int countDirs = 0;
		LinkedHashMap<String, DropBoxExplicitMetadata> metadataMap = new LinkedHashMap<String, DropBoxExplicitMetadata>();
		FolderMetadata lastDmd = null;
		System.out.println("==========READING DROPBOX =========>>");
		// Get files and folder metadata from Dropbox root directory
		result = client.files().listFolderBuilder(workingRootPath).withRecursive(true).start();
		int iter = 0;
		int rec = 0;
		while (true) {
			if(DEBUG){
				System.out.print("DB[" + iter + "]: ");
			}
			
			String lastPath = null;
			for (Metadata md : result.getEntries()) {
				lastPath = md.getPathDisplay();
				if(DEBUG){
					System.out.println("DB[" + iter + "].[" + rec + "]: \t->" + lastPath);				
					if (md.getPathDisplay().length() > workingRootPath.length()) {
						System.out.print("\t[r]");
					} else {
						System.out.print("\t[X]");
					}
				}

				if (md instanceof FolderMetadata) {
					FolderMetadata dmd = (FolderMetadata) md;
					if(DEBUG){
						System.out.print("\t[d]\t[" + dmd.getId() + "]\t\"" + md.getPathDisplay() + "\"");
					}
					final DropBoxExplicitFolderMetadata demd = new DropBoxExplicitFolderMetadata(dmd);
					final LocalFileView ld = localFileViewMap.get(demd.getFolder().getPathDisplay());
					if (ld != null) {
						ld.setExistInCloud(true);
					}
					metadataMap.put(demd.getFolder().getPathDisplay(), demd);
					if (lastDmd != null && demd.getFolder().getPathDisplay().startsWith(lastDmd.getPathDisplay())) {
						demd.setPid(lastDmd.getId());
						lastDmd = dmd;
					} else {
						lastDmd = dmd;
					}
					countDirs++;
				} else if (md instanceof FileMetadata) {
					FileMetadata fmd = (FileMetadata) md;
					if(DEBUG){
						System.out.print("\t[-]\t[" + fmd.getId() + "]\t\"" + fmd.getPathDisplay() + "\"\t(" + fmd.getSize() + ")");
					}
					final DropBoxExplicitFileMetadata femd = new DropBoxExplicitFileMetadata(fmd);
					final LocalFileView ld = localFileViewMap.get(femd.getFile().getPathDisplay());
					if (ld != null) {
						ld.setExistInCloud(true);
					}
					metadataMap.put(femd.getFile().getPathDisplay(), femd);
					if (lastDmd != null && femd.getFile().getPathDisplay().startsWith(lastDmd.getPathDisplay())) {
						femd.setPid(lastDmd.getId());
					} else {

					}
				}
				if(DEBUG){
					System.out.println("");
				}
				countFiles++;
				rec++;
			}

			if (!result.getHasMore()) {
				break;
			}
			iter++;
			result = client.files().listFolderContinue(result.getCursor());
		}
		System.out.println("==========FINISHING READING DROPBOX=========>>");
		System.out.println("\tDROPBOX END READ WITH: " + countFiles + " FILES IN " + countDirs + " DIRS.");
		printMemoryUsage();
		
		if (listFiles) {
			System.exit(0);
		}

		localFile = null;

		List<DropBoxExplicitMetadata> dbefmUpdateByUpload = new ArrayList<DropBoxExplicitMetadata>();
		LinkedList<DropBoxExplicitMetadata> dbefmUpdateByDelete = new LinkedList<DropBoxExplicitMetadata>();

		for (DropBoxExplicitMetadata emd : metadataMap.values()) {
			FolderMetadata dmd = null;
			FileMetadata fmd = null;
			SyncControlRecord xt6 = syncBeforeControlRecordMap.get(emd.getPathDisplay());

			if (emd.isDirectpory()) {
				dmd = emd.getFolder();
				
				if(DEBUG) {
					System.out.print("d[" + dmd.getId() + "] \"" + dmd.getPathDisplay() + "\" ");
				}

				localFile = new File(localRootPath, dmd.getPathDisplay());
				if (!localFile.exists()) {
					if (xt6 == null) {
						boolean created = localFile.mkdirs();						
						if(DEBUG) System.out.println(" [m] ");
					} else {
						FolderMetadata deleteMD = dmd;
						if(DEBUG) System.out.print("[r] add for remove dir");
						dbefmUpdateByDelete.addFirst(new DropBoxExplicitFolderMetadata(deleteMD));
					}
				} else {
					if(DEBUG) System.out.println(" [ ] ");
				}
			} else {
				fmd = emd.getFile();

				String md5 = null;
				long sfm = fmd.getServerModified().getTime();
				long lfm = 0;
				if(DEBUG) System.out.println("-[" + fmd.getId() + "] \"" + fmd.getPathDisplay() + "\"");
				if(DEBUG) System.out.print("\t\t\t\t\tsize:" + fmd.getSize() + "\tTsm:" + sdf.format(sfm));
				localFile = new File(localRootPath, fmd.getPathDisplay());

				if (!localFile.exists()) {
					if (xt6 == null) {
						if(DEBUG) System.out.print("\tD");
						DbxDownloader<FileMetadata> download = client.files().download(fmd.getPathDisplay());
						FileOutputStream fos = new FileOutputStream(localFile);
						FileMetadata dMD = download.download(fos);
						fos.close();
						boolean sd = localFile.setLastModified(dMD.getServerModified().getTime());
						md5 = calculateMD5(localFile);
						if(DEBUG) System.out.print("[v] {" + md5 + "} >> sfm=" + sdf.format(dMD.getServerModified()));
					} else {
						FileMetadata deleteMD = fmd;
						if(DEBUG) System.out.print("[r] add for remove");
						dbefmUpdateByDelete.addFirst(new DropBoxExplicitFileMetadata(deleteMD));
					}
				} else {
					lfm = localFile.lastModified();
					md5 = calculateMD5(localFile);
					if(DEBUG) System.out.println("\tL[_] >>");
					if (xt6 != null) {
						if(DEBUG) System.out.println("\t\t\t\t\tSIZE:" + xt6.getSize() + "\tLAT:" + sdf.format(xt6.getLat()) + "\t" + xt6.getMd5());
					}
					if(DEBUG) System.out.print("\t\t\t\t\tsize:" + localFile.length() + "\tlat:" + sdf.format(lfm) + "\t" + md5);

					boolean shouldDownload = (sfm - lfm) > 0;
					boolean shouldUpload = (sfm - lfm) < 0;
					boolean sameSize = (localFile.length() == fmd.getSize());

					if (sameSize && numSync == 0) {
						md5 = calculateMD5(localFile);
						if(DEBUG) System.out.print("[[=]] 1st sync.");
						boolean sd = localFile.setLastModified(sfm);
					} else if (shouldDownload) {
						if(DEBUG) System.out.print("\tD");
						DbxDownloader<FileMetadata> download = client.files().download(fmd.getPathDisplay());
						FileOutputStream fos = new FileOutputStream(localFile);
						FileMetadata mdU = download.download(fos);
						fos.close();
						boolean sd = localFile.setLastModified(sfm);
						md5 = calculateMD5(localFile);

						if(DEBUG) System.out.print("[V] size:" + mdU.getSize() + "\t" + md5 + "}");
					} else if (shouldUpload) {
						if(DEBUG) System.out.print("\tU");
						md5 = calculateMD5(localFile);
						FileInputStream fis = new FileInputStream(localFile);
						final FileMetadata uploadMD = client.files().uploadBuilder(fmd.getPathDisplay()).
								withMode(WriteMode.OVERWRITE).
								uploadAndFinish(fis);

						xt6.setLat(uploadMD.getServerModified().getTime());
						final DropBoxExplicitFileMetadata dbefmU = new DropBoxExplicitFileMetadata(uploadMD);
						dbefmU.setMd5(md5);

						dbefmUpdateByUpload.add(dbefmU);
						localFile.setLastModified(uploadMD.getServerModified().getTime());

						if(DEBUG) System.out.print("[^] >> md5=" + md5 + ",[" + uploadMD.getId() + "], sfm=" + sdf.format(uploadMD.getServerModified().getTime()) + "(" + uploadMD.getServerModified().getTime() + ")"
								+ ", cfm=" + sdf.format(uploadMD.getClientModified().getTime()) + "(" + uploadMD.getClientModified().getTime() + ")");
					} else {
						md5 = calculateMD5(localFile);

						if(DEBUG) System.out.print("[=] md5=" + md5);
						if (xt6 != null && (!xt6.getMd5().equals(md5))) {
							if(DEBUG) System.out.print(" but binary NOT EQUALS by MD5");
						}
					}

				}
				emd.setMd5(md5);
				if(DEBUG) System.out.println(" $");
			}
		}

		for (DropBoxExplicitMetadata dbemf : dbefmUpdateByUpload) {
			metadataMap.remove(dbemf.getFile().getPathDisplay());
			if(DEBUG) System.out.println("Updating File list by upload: " + dbemf.getId() + "->" + dbemf);
			metadataMap.put(dbemf.getFile().getPathDisplay(), dbemf);
		}

		while (!dbefmUpdateByDelete.isEmpty()) {
			final DropBoxExplicitMetadata removeFirst = dbefmUpdateByDelete.removeFirst();
			metadataMap.remove(removeFirst.getPathDisplay());
			client.files().delete(removeFirst.getPathDisplay());
			if(DEBUG) System.out.println("\tDropBox rm " + removeFirst.getPathDisplay());
		}

		System.out.println("After Downlad (Files existing before Sync){");
		LinkedList<File> fileDeleteStack = new LinkedList<File>();
		for (LocalFileView lfv : localFileViewMap.values()) {
			localFile = new File(localRootPathDir,lfv.getPath());
			String cloudPath = lfv.getPath().replace(localRootPath, "");
			if (!cloudPath.startsWith("/")) {
				cloudPath = "/" + cloudPath;
			}
			if(DEBUG) System.out.print("\t=>[" + lfv.getPath() + "] =>[" + cloudPath + "]");
			SyncControlRecord rx9 = syncBeforeControlRecordMap.get(cloudPath);
			DropBoxExplicitMetadata rxD9 = metadataMap.get(cloudPath);

			if (rx9 != null) {
				// Antes estaba  en la NUBE
				if (rxD9 == null) {
					// Ahora NO esta en la NUBE
					File fileToDelete = new File(lfv.getPath());
					if(DEBUG) System.out.print("\tAdded for rm");
					fileDeleteStack.addFirst(fileToDelete);
				} else {
					// Ahora YA esta en la NUBE
					if(DEBUG) System.out.print("\t - TODO OK");
				}
			} else // Antes NO estaba  en la NUBE
			if (rxD9 == null) {
				// Ahora NO esta en la NUBE					
				if(DEBUG) System.out.print("\tN\tU\t");
				if (localFile.isDirectory()) {
					final FolderMetadata createFolder = client.files().createFolder(cloudPath);
					final DropBoxExplicitFolderMetadata dropBoxExplicitFolfderMetadata = new DropBoxExplicitFolderMetadata(createFolder);

					metadataMap.put(cloudPath, dropBoxExplicitFolfderMetadata);

					if(DEBUG) System.out.print("[m]");
				} else {
					FileInputStream fis = new FileInputStream(localFile);
					final FileMetadata uploadMD = client.files().uploadBuilder(cloudPath).
							withMode(WriteMode.ADD).
							uploadAndFinish(fis);
					final DropBoxExplicitFileMetadata dropBoxExplicitFileMetadata = new DropBoxExplicitFileMetadata(uploadMD);
					String md5 = calculateMD5(localFile);
					dropBoxExplicitFileMetadata.setMd5(md5);

					metadataMap.put(cloudPath, dropBoxExplicitFileMetadata);
					localFile.setLastModified(uploadMD.getServerModified().getTime());
					if(DEBUG){
						System.out.print("[^] >> md5=" + md5 + ",[" + uploadMD.getId()
								+ "], sfm=" + sdf.format(uploadMD.getServerModified().getTime()) + "(" + uploadMD.getServerModified().getTime() + ")"
								+ ", cfm=" + sdf.format(uploadMD.getClientModified().getTime()) + "(" + uploadMD.getClientModified().getTime() + ")");
					}
				}
			} else // Ahora YA esta en la NUBE
			if (numSync == 0) {
				if(DEBUG) System.out.print("[=] 1st sync.");
			} else if (numSync > 0) {
				File fileToDelete = new File(lfv.getPath());
				if (fileToDelete.isDirectory()) {
					FileUtils.deleteDirectory(fileToDelete);
					if(DEBUG) System.out.print("\trm 2 dir:ok");
				} else {
					boolean xxd = fileToDelete.delete();
					if(DEBUG) System.out.print("\trm 2 OK?" + xxd);
				}
			}
			if(DEBUG) System.out.println("");
		}
		System.out.println("}");

		while (!fileDeleteStack.isEmpty()) {
			File fileToDelete = fileDeleteStack.removeFirst();
			System.out.print("\trm " + fileToDelete);
			boolean xxd = fileToDelete.delete();
			System.out.println("\t?" + xxd);
		}

		PrintStream psProprs = new PrintStream(new File(configRootFile, CONTROLSYNC_FILE));
		for (DropBoxExplicitMetadata emd : metadataMap.values()) {
			psProprs.println(emd.toString());
		}

		propertiesX.setProperty("LAST_UPDATE", sdf.format(new Date()));
		propertiesX.setProperty("NUM_SYNC", String.valueOf(++numSync));

		propertiesX.store(new FileOutputStream(propertiesFile), "Update ant end");
		System.out.println("\t==========DONE.");
	}

	private static void printMemoryUsage() {
		final long tm = Runtime.getRuntime().totalMemory();
		final long fm = Runtime.getRuntime().freeMemory();
		System.out.println("\t--------------------------------------------");		
		System.out.println("\t   TOTAL_MEMORY : " + prettyBytes(tm));		
		System.out.println("\t    USED_MEMORY : " + prettyBytes(tm - fm));
		System.out.println("\t    FREE_MEMORY : " + prettyBytes(fm));
		System.out.println("\t--------------------------------------------");
	}

	private static MessageDigest md = null;

	private static byte buffer[] = null;

	private static String calculateMD5(File file) throws IOException {
		return "md5NotUsedBySpeed";
		//return DigestUtils.md5Hex(new FileInputStream(file));
	}
	
	private static String prettyBytes(long bytes){
		StringBuilder sb = new StringBuilder();
		DecimalFormat df=new DecimalFormat("0.0000");
		if(bytes <= 1024){
			sb.append(df.format(bytes)).append(" B");
		} else if(bytes > 1024 && bytes <=1048576){ // KB
			sb.append(df.format((double)bytes/1024.0)).append(" KB");
		} else if(bytes > 1048576 && bytes <= 1073741824){ // MB
			sb.append(df.format((double)bytes/1048576.0)).append(" MB");
		}  else if(bytes > 1073741824){ // GB
			sb.append(df.format((double)bytes/1073741824.0)).append(" GB");
		}
		
		return sb.toString();
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

	private static void appendToListDir(LinkedHashMap<String, LocalFileView> listDist, File dir, File root, String wd) {
		File[] listFiles = dir.listFiles();
		if (listFiles != null) {
			for (File f : listFiles) {

				String unixPath = f.getPath().replace("\\", "/");
				if (!root.getPath().equals(".")) {
					unixPath = unixPath.replace(root.getPath(), "");
				}

				//System.out.println("\t=>appendToListDir("+root.getPath()+")["+unixPath+"], wd=["+wd+"]");
				if (f.isDirectory()) {
					if (unixPath.contains(wd)) {
						listDist.put(unixPath, new LocalFileView("d", unixPath, null, null, null, false));
					}
					appendToListDir(listDist, f, root, wd);
				} else {
					listDist.put(unixPath, new LocalFileView("f", unixPath, null, f.length(), f.lastModified(), false));
				}
			}
		}
	}
}

class LocalFileView {

	private String type;
	private String path;
	private String md5;
	private Long size;
	private Long lat;
	private boolean existInCloud;

	public LocalFileView(String type, String path, String md5, Long size, Long lat, boolean existInCloud) {
		this.type = type;
		this.path = path;
		this.md5 = md5;
		this.size = size;
		this.lat = lat;
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
		if (type.equals("f")) {
			return type + "|" + path + "|" + size + "|" + Main.sdf.format(lat) + "|" + (existInCloud ? "*" : "_");
		} else if (type.equals("d")) {
			return type + "|" + path + "|" + (existInCloud ? "*" : "_");
		} else {
			return null;
		}
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
	 * @return the lat
	 */
	public Long getLat() {
		return lat;
	}

	/**
	 * @param lat the lat to set
	 */
	public void setLat(Long lat) {
		this.lat = lat;
	}

}

class SyncControlRecord {

	private String t;
	private String pid;
	private String id;
	private String path;
	private Long size;
	private Long lat;
	private String md5;

	public SyncControlRecord(String line) {
		String r[] = line.split("\\|");
		//final String er = "SyncControlRecord line=\""+line+"\"\t, split="+Arrays.asList(r);
		//System.err.println(er);

		this.t = r[0];
		if (r[1] != null && r[1].length() > 0) {
			this.pid = r[1];
		}
		this.id = r[2];
		this.path = r[3];

		if (r.length > 4) {
			if (r[4] != null && r[4].length() > 0) {
				this.size = new Long(r[4]);
			}
			if (r[5] != null && r[5].length() > 0) {
				this.lat = new Long(r[5]);
			}
			if (r[6] != null && r[6].length() > 0) {
				this.md5 = r[6];
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
	 * @return the lat
	 */
	public Long getLat() {
		return lat;
	}

	/**
	 * @param lat the lat to set
	 */
	public void setLat(Long lat) {
		this.lat = lat;
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
		if (pid != null) {
			sb.append(pid);
		} else {
			sb.append("");
		}
		sb.append("|");
		sb.append(id);
		sb.append("|");
		sb.append(path);
		sb.append("|");
		if (size != null) {
			sb.append(size);
		} else {
			sb.append("");
		}
		sb.append("|");
		if (lat != null) {
			sb.append(lat);
		} else {
			sb.append("");
		}
		sb.append("|");
		if (md5 != null) {
			sb.append(md5);
		} else {
			sb.append("");
		}

		return sb.toString();

	}

}

interface DropBoxExplicitMetadata {

	boolean isDirectpory();

	String getId();

	String getPid();

	void setPid(String pid);

	FolderMetadata getFolder();

	FileMetadata getFile();

	void setMd5(String md5);

	String getMd5();

	String getPathDisplay();
}

class DropBoxExplicitFolderMetadata implements DropBoxExplicitMetadata {

	private FolderMetadata dmd;
	private String md5;
	private String pid;

	public DropBoxExplicitFolderMetadata(Metadata md) {
		dmd = (FolderMetadata) md;
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
		if (getPid() != null) {
			sb.append(getPid());
		} else {
			sb.append("");
		}
		sb.append("|");
		sb.append(dmd.getId());
		sb.append("|");
		sb.append(dmd.getPathDisplay());
		sb.append("|||");
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
		return this.getId().equals(((DropBoxExplicitMetadata) obj).getId());
	}

	@Override
	public String getPathDisplay() {
		return this.dmd.getPathDisplay();
	}
}

class DropBoxExplicitFileMetadata implements DropBoxExplicitMetadata {

	private FileMetadata fmd;
	private String md5;
	private String pid;

	public DropBoxExplicitFileMetadata(Metadata md) {
		fmd = (FileMetadata) md;
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
		if (getPid() != null) {
			sb.append(getPid());
		} else {
			sb.append("");
		}
		sb.append("|");
		sb.append(fmd.getId());
		sb.append("|");
		sb.append(fmd.getPathDisplay());
		sb.append("|");
		sb.append(fmd.getSize());
		sb.append("|");
		if (fmd.getServerModified() != null) {
			sb.append(fmd.getServerModified().getTime());
		} else {
			sb.append("");
		}
		sb.append("|");
		if (getMd5() != null) {
			sb.append(getMd5());
		} else {
			sb.append("");
		}

		return sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		return this.getId().equals(((DropBoxExplicitMetadata) obj).getId());
	}

	@Override
	public String getPathDisplay() {
		return this.fmd.getPathDisplay();
	}
}
