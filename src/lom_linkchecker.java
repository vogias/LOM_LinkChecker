import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Created with IntelliJ IDEA. User: Enayat Rajabi (university of ALcala de
 * Henares) Date: 7/31/13 Time: 11:02 AM To change this template use File |
 * Settings | File Templates.
 */
public class lom_linkchecker {

	private static final Logger slf4jLogger = LoggerFactory.getLogger(lom_linkchecker.class);
	private final static String QUEUE_NAME = "link_checking";
	private int deadLinks = 0;
	private int notWellFormed = 0;
	private int liveLinks = 0;
	private int recordsNumber = 0;

	// // ------------ Get Connection from MySQL --------------------------
	// private static Connection Get_Connection(String databaseName,
	// String userName, String passWord) throws Exception {
	// try {
	//
	// System.out.println("Connecting to a selected database...");
	// // Name of database
	// String connectionURL = "jdbc:mysql://localhost:3306/"
	// + databaseName;
	// Connection connection = null;
	// Class.forName("com.mysql.jdbc.Driver").newInstance();
	//
	// // Connection username and password
	// connection = DriverManager.getConnection(connectionURL, userName,
	// passWord);
	// System.out.println("Connected database successfully...");
	// return connection;
	// } catch (SQLException e) {
	// System.out.println("Can not connect to the database!");
	// System.out
	// .println("Please be sure that you've created the database, the username
	// and password are correct, and you are using mysql DB");
	// System.exit(1);
	// throw e;
	// } catch (Exception e) {
	// System.out.println("Connection problem...");
	// System.exit(1);
	// throw e;
	// }
	// }

	// ------------ Check availability of each URL --------------------------
	public int URLChecker(String link) {

		HttpURLConnection urlconn = null;

		// int res = -1;
		// String msg = null;
		try {

			URL url = new URL(link);

			String protocol = url.getProtocol();

			// System.out.println(url);
			if (!protocol.equals("http") && !protocol.equals("https")) {
				// HttpsURLConnection hc =
				// (HttpsURLConnection)url.openConnection();
				// hc.setConnectTimeout(5000);
				// hc.setReadTimeout(5000);
				// hc.disconnect();
				return -2;
			}

			urlconn = (HttpURLConnection) url.openConnection();
			urlconn.setConnectTimeout(10000);
			urlconn.setReadTimeout(10000);
			urlconn.setRequestMethod("GET");

			urlconn.connect();
			String redirlink = urlconn.getHeaderField("Location");

			// System.out.println(urlconn.getHeaderFields());

			if (redirlink != null && !url.toExternalForm().equals(redirlink)) {
				return URLChecker(redirlink);
			} else
				return urlconn.getResponseCode();

		} catch (Exception e) {
			System.out.println(e.getMessage());
			if (e.getMessage().contains("timeout"))
				return -3;
			if (e.getMessage().contains("refused"))
				return -4;
			else
				return -5;
		} finally {

			if (urlconn != null)
				urlconn.disconnect();
		}

	}

	// ------------ OPEN XML FILE AND READ AN ELEMENT THAT IS UNDER
	// ROOT->ELEMENT --------------------------
	String GetURLFromXML(String fileName, String root, String element) {
		String urlCheck = null;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			File file = new File(fileName);
			if (file.exists()) {

				Document doc = db.parse(file);
				Element docEle = doc.getDocumentElement();
				// Print root element of the document
				// System.out.println("Root element of the document: "
				// + docEle.getNodeName());

				try {
					// Root element --- here is general ---
					// C:\Enayat\ODS_Resources\SecondCycle\RESURSI
					NodeList ElementList = null;
					if (root.equals("oai_dc:dc"))
						ElementList = doc.getElementsByTagName(root);
					else
						ElementList = docEle.getElementsByTagName(root);

					if (ElementList != null && ElementList.getLength() > 0) {
						for (int i = 0; i < ElementList.getLength(); i++) {
							Node node = ElementList.item(i);
							if (node.getNodeType() == Node.ELEMENT_NODE) {
								Element e = (Element) node;
								// System.out.println("Identifier====="+e.getElementsByTagName("dc:identifier").item(0).getTextContent()+"===========");
								try {
									// Link element= identifier. entry
									NodeList nodeList = e.getElementsByTagName(element);
									urlCheck = nodeList.item(0).getChildNodes().item(0).getNodeValue();
								} catch (Exception ee) {
									urlCheck = "Error";
								}
							}
						}
					} else {
						urlCheck = "Error";
					}
				} catch (Exception eg) {
					urlCheck = "Error";
				}
			}
		} catch (Exception e) {
			urlCheck = "Error";
		}
		return urlCheck;
	}

	// ------------ INSERT RECORD INTO LOG TABLE (URL, Dead or Live?, the
	// filename, connection name) --------------------------
	public synchronized void insertRecord(String URL, String Result, String FileName, Connection conn, String tbName) {
		Statement stmt = null;
		FileName = FileName.replace('\\', '/');
		try {
			stmt = conn.createStatement();
			String sql = "INSERT INTO " + tbName + " (URL,Result,FileName)" + "VALUES ('" + URL + "','" + Result + "','"
					+ FileName + "');";
			System.out.print(sql);
			stmt.executeUpdate(sql);
		} catch (SQLException se) {
			// Handle errors for JDBC
			se.printStackTrace();
		} catch (Exception e) {
			// Handle errors for Class.forName
			e.printStackTrace();
		} finally {
		} // end try
	}

	public synchronized void raiseNotWellFormed() {
		notWellFormed++;
	}

	public synchronized void raiseLiveLinks() {
		liveLinks++;
	}

	public synchronized void raiseDeadLinks() {
		deadLinks++;
	}

	public int getNotWellFormed() {
		return notWellFormed;
	}

	public int getDeadLinks() {
		return deadLinks;
	}

	public int getLiveLinks() {
		return liveLinks;
	}

	/*
	 * void checkLink(File folder, String dbName, String tbName, String
	 * userName, String passWord, File brokenFolder) { File[] listOfFiles;
	 * 
	 * int fileNumber = 0;
	 * 
	 * // Connection conn = null;
	 * 
	 * Properties props = new Properties(); int threadPoolSize = 1;
	 * 
	 * try { props.load(new FileInputStream("configure.properties"));
	 * threadPoolSize = Integer.parseInt(props
	 * .getProperty(Constants.threadPoolSize)); } catch (FileNotFoundException
	 * e1) { // TODO Auto-generated catch block e1.printStackTrace();
	 * System.exit(-1); } catch (IOException e1) { // TODO Auto-generated catch
	 * block e1.printStackTrace(); System.exit(-1); } catch (ClassCastException
	 * e) { // TODO: handle exception
	 * System.err.println("Wrong thread pool size value..."); System.exit(-1); }
	 * 
	 * // try { // conn = Get_Connection(dbName, userName, passWord); // } catch
	 * (SQLException se) { // se.printStackTrace(); // } catch (Exception e) {
	 * // e.printStackTrace(); // }
	 * 
	 * FilenameFilter filter = new FilenameFilter() { public boolean accept(File
	 * dir, String name) { return name.endsWith(".xml") ||
	 * name.endsWith(".XML"); } };
	 * 
	 * try { listOfFiles = folder.listFiles(filter); fileNumber =
	 * listOfFiles.length; if (fileNumber == 0) {
	 * System.out.println("No XML files found in the selected folder"); try { if
	 * (conn != null) conn.close(); } catch (SQLException se) { // Handle errors
	 * for JDBC se.printStackTrace(); } System.exit(1); }
	 * 
	 * System.out.println("processing " + fileNumber + " files ..."); String
	 * provider = folder.getName();
	 * 
	 * int availableProcessors = Runtime.getRuntime() .availableProcessors();
	 * System.out.println("Available cores:" + availableProcessors);
	 * System.out.println("Thread Pool size:" + threadPoolSize); ExecutorService
	 * executor = Executors .newFixedThreadPool(threadPoolSize);
	 * 
	 * long start = System.currentTimeMillis(); for (int i = 0; i < fileNumber;
	 * i++) {
	 * 
	 * WorkerDB worker = new WorkerDB(provider, listOfFiles[i], this, conn,
	 * tbName, brokenFolder, slf4jLogger); executor.execute(worker);
	 * 
	 * } executor.shutdown();
	 * 
	 * executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS); long end
	 * = System.currentTimeMillis(); long diff = end - start;
	 * System.out.println("Duration:" + diff + "ms"); try { if (conn != null)
	 * conn.close(); } catch (SQLException se) { // Handle errors for JDBC
	 * se.printStackTrace(); } System.out
	 * .println(" --------------------Finsished! -----------------------------"
	 * ); System.out.println(" Total number of checked records=" +
	 * getRecordsNumber()); System.out.println(" Number of broken links=" +
	 * getDeadLinks()); System.out.println(" Number of not well-formed=" +
	 * getNotWellFormed()); System.out
	 * .println(" ------------------------------------------------------------"
	 * );
	 * 
	 * } catch (Exception NotFolder) { System.out.println("Un-expected Error ");
	 * // throw NotFolder; } }
	 */

	void checkLink(File folder, File brokenFolder) {
		File[] listOfFiles;

		int fileNumber = 0;

		Properties props = new Properties();
		int threadPoolSize = 1;

		try {
			props.load(new FileInputStream("configure.properties"));
			threadPoolSize = Integer.parseInt(props.getProperty(Constants.threadPoolSize));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.exit(-1);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.exit(-1);
		} catch (NumberFormatException e) {
			// TODO: handle exception
			System.err.println("Wrong thread pool size value...");
			System.exit(-1);
		}

		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".xml") || name.endsWith(".XML");
			}
		};

		try {
			listOfFiles = folder.listFiles(filter);
			fileNumber = listOfFiles.length;
			if (fileNumber == 0) {
				System.out.println("No XML files found in the selected folder");

				System.exit(1);
			}

			System.out.println("processing " + fileNumber + " files ...");
			String provider = folder.getName();

			int availableProcessors = Runtime.getRuntime().availableProcessors();
			System.out.println("Available cores:" + availableProcessors);
			System.out.println("Thread Pool size:" + threadPoolSize);
			ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);

			long start = System.currentTimeMillis();

			for (int i = 0; i < fileNumber; i++) {

				WorkerFS worker = new WorkerFS(provider, listOfFiles[i], this, brokenFolder, slf4jLogger, QUEUE_NAME);
				executor.execute(worker);

			}
			executor.shutdown();

			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			long end = System.currentTimeMillis();
			long diff = end - start;

			System.out.println(" --------------------Finsished! -----------------------------");
			System.out.println(" Duration:" + diff + "ms");
			System.out.println(" Total number of checked records=" + getRecordsNumber());
			System.out.println(" Number of broken links=" + getDeadLinks());
			System.out.println(" Number of not well-formed=" + getNotWellFormed());
			System.out.println(" ------------------------------------------------------------");

		} catch (Exception NotFolder) {
			System.out.println("Un-expected Error ");
			// throw NotFolder;
		}
	}

	public synchronized void raiseRecordsNumber() {
		recordsNumber++;
	}

	public int getRecordsNumber() {
		return recordsNumber;
	}

	public static void main(String args[]) {
		lom_linkchecker ods_linkchecker = new lom_linkchecker();

		File metadataFolder;
		// String username;
		// String password;
		File brokenFolder;

		// if (args.length == 4) {
		// metadataFolder = new File(args[0]);
		// username = args[1];
		// password = args[2];
		// brokenFolder = new File(args[3]);
		// if (!metadataFolder.exists()) {
		// System.out.println("-------------------------------------");
		// System.out.println("Error! the folder does not exist-->"
		// + metadataFolder.getAbsolutePath());
		// System.out.println("-------------------------------------");
		// System.exit(1);
		// }
		// if (!brokenFolder.exists()) {
		// System.out.println("-------------------------------------");
		// System.out.println("Error! the folder does not exist--->"
		// + brokenFolder.getAbsolutePath());
		// System.out.println("-------------------------------------");
		// System.exit(1);
		// }
		// ods_linkchecker.checkLink(metadataFolder, "linkchecker", "log",
		// username, password, brokenFolder);
		// } else

		if (args.length == 2) {
			metadataFolder = new File(args[0]);
			brokenFolder = new File(args[1]);
			if (!metadataFolder.exists()) {
				System.out.println("-------------------------------------");
				System.out.println("Error! the folder does not exist-->" + metadataFolder.getAbsolutePath());
				System.out.println("-------------------------------------");
				System.exit(1);
			}
			if (!brokenFolder.exists()) {
				System.out.println("-------------------------------------");
				System.out.println("Error! the folder does not exist--->" + brokenFolder.getAbsolutePath());
				System.out.println("-------------------------------------");
				System.exit(1);
			}
			ods_linkchecker.checkLink(metadataFolder, brokenFolder);
		} else {
			System.err.println("Wrong number of input arguments...");
			System.err
					.println("For DB log creation arguments can be: metadataFolder--username--password--brokenFolder");
			System.err.println("For FS log creation arguments can be: metadataFolder--brokenFolder");
		}

	}
}
