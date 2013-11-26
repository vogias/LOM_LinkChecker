import java.io.File;
import java.io.FilenameFilter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

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

	private static final Logger slf4jLogger = LoggerFactory
			.getLogger(lom_linkchecker.class);

	// ------------ Get Connection from MySQL --------------------------
	public static Connection Get_Connection(String databaseName,
			String userName, String passWord) throws Exception {
		try {
			System.out.println("Connecting to a selected database...");
			// Name of database
			String connectionURL = "jdbc:mysql://localhost:3306/"
					+ databaseName;
			Connection connection = null;
			Class.forName("com.mysql.jdbc.Driver").newInstance();

			// Connection username and password
			connection = DriverManager.getConnection(connectionURL, userName,
					passWord);
			System.out.println("Connected database successfully...");
			return connection;
		} catch (SQLException e) {
			System.out.println("Can not connect to the database!");
			System.out
					.println("Please be sure that you've created the database, the username and password are correct, and you are using mysql DB");
			System.exit(1);
			throw e;
		} catch (Exception e) {
			System.out.println("Connection problem...");
			System.exit(1);
			throw e;
		}
	}

	// ------------ Check availability of each URL --------------------------
	private static int URLChecker(String link) {

		HttpURLConnection urlconn = null;

		int res = -1;
		String msg = null;
		try {

			URL url = new URL(link);

			String protocol = url.getProtocol();
			// System.out.println(url);
			if (!protocol.equals("http")) {
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
									NodeList nodeList = e
											.getElementsByTagName(element);
									urlCheck = nodeList.item(0).getChildNodes()
											.item(0).getNodeValue();
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
	public void insertRecord(String URL, String Result, String FileName,
			Connection conn, String tbName) {
		Statement stmt = null;
		FileName = FileName.replace('\\', '/');
		try {
			stmt = conn.createStatement();
			String sql = "INSERT INTO " + tbName + " (URL,Result,FileName)"
					+ "VALUES ('" + URL + "','" + Result + "','" + FileName
					+ "');";
			System.out.print(sql);
			stmt.executeUpdate(sql);
		} catch (SQLException se) {
			// Handle errors for JDBC
			se.printStackTrace();
		} catch (Exception e) {
			// Handle errors for Class.forName
			e.printStackTrace();
		} finally {
		}// end try
	}

	void checkLink(File folder, String dbName, String tbName, String userName,
			String passWord, File brokenFolder) {
		File[] listOfFiles;

		int fileNumber = 0, deadLinks = 0, liveLinks = 0, notWellFormed = 0;
		String fileName = null;
		Connection conn = null;
		int recordsNumber = 0;

		try {
			conn = Get_Connection(dbName, userName, passWord);
		} catch (SQLException se) {
			se.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
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
				try {
					if (conn != null)
						conn.close();
				} catch (SQLException se) {
					// Handle errors for JDBC
					se.printStackTrace();
				}
				System.exit(1);
			}

			System.out.println("processing " + fileNumber + " files ...");
			String provider = folder.getName();
			for (int i = 0; i < fileNumber; i++) {

				StringBuffer logString = new StringBuffer();

				logString.append(provider);

				System.out.println("processing " + (i + 1) + "th file ...");
				fileName = listOfFiles[i].getPath().toString();
				String name = listOfFiles[i].getName();
				name = name.substring(0, name.indexOf(".xml"));

				logString.append(" "+name);

				System.out.println("checking FileName= " + fileName);
				String metadataURL = (GetURLFromXML(fileName, "technical",
						"location").toString()).replace("'", "\\'");
				if (metadataURL.equals("Error"))
					metadataURL = (GetURLFromXML(fileName, "lom:technical",
							"lom:location").toString());
				if (metadataURL.equals("Error"))
					metadataURL = (GetURLFromXML(fileName, "oai_dc:dc",
							"dc:identifier").toString());

				if (metadataURL.equals("Error")) {
					try {
						System.out.println("--------------------File #(" + i
								+ ")-----------------------------");
						insertRecord(metadataURL, "NotWellFormed", fileName,
								conn, tbName);

						logString.append(" " + "NotWellFormed");

						System.out.println("--------------------File #(" + i
								+ ")-----------------------------");
						System.out.println("No metadata element found!");

						notWellFormed += 1;

						// -------------------------Move files
						// ----------------------------------
						// File newFile =new
						// File(brokenFolder.toPath()+"\\"+listOfFiles[i].getName());
						File newFile = new File(brokenFolder.getPath(),
								listOfFiles[i].getName());

						Files.move(listOfFiles[i].toPath(), newFile.toPath());
						System.out.println("File=" + listOfFiles[i].toPath()
								+ " was moved to" + brokenFolder);
						// ----------------------------------------------------------------------
					} catch (Exception e) {
						System.out
								.println("Error in moving file in NotWellFormed section");
						System.exit(2);
					}
				} else {
					System.out.println("Found URL=" + metadataURL);

					int result = URLChecker(metadataURL);
					if (result != 200) {
						try {
							System.out.println("--------------------File #("
									+ i + ")-----------------------------");
							insertRecord(metadataURL, Integer.toString(result),
									fileName, conn, tbName);
							System.out
									.println("----------------------------------------------------");
							deadLinks += 1;

							// -------------------------Move files
							// ----------------------------------
							System.out.println("File source="
									+ listOfFiles[i].toPath());
							System.out.println("File Distination="
									+ brokenFolder.toPath());

							// File newFile =new
							// File(brokenFolder.toPath()+"\\"+listOfFiles[i].getName());
							File newFile = new File(brokenFolder.getPath(),
									listOfFiles[i].getName());
							Files.move(listOfFiles[i].toPath(),
									newFile.toPath());
							System.out.println("File="
									+ listOfFiles[i].toPath() + " was moved to"
									+ brokenFolder);
							logString.append(" " + "DeadLink");
							// ----------------------------------------------------------------------

						} catch (Exception e) {
							System.out
									.println("Error in moving file in result<>200 section");
						}
					} else {
						try {
							System.out.println("--------------------File #("
									+ i + ")-----------------------------");
							insertRecord(metadataURL, Integer.toString(result),
									fileName, conn, tbName);
							System.out
									.println("--------------------------------------------------------");
							liveLinks += 1;
							logString.append(" " + "Livelink");
						} catch (Exception e) {
							System.out
									.println("Error in inserting record  in result=200 section");
						}
					}
				}
				recordsNumber = i + 1;

				slf4jLogger.info(logString.toString());
			}
			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				// Handle errors for JDBC
				se.printStackTrace();
			}
			System.out
					.println(" --------------------Finsished! -----------------------------");
			System.out.println(" Total number of checked records="
					+ recordsNumber);
			System.out.println(" Number of broken links=" + deadLinks);
			System.out.println(" Number of not well-formed=" + notWellFormed);
			System.out
					.println(" ------------------------------------------------------------");

		} catch (Exception NotFolder) {
			System.out.println("Un-expected Error ");
			// throw NotFolder;
		}
	}

	public static void main(String args[]) {
		lom_linkchecker ods_linkchecker = new lom_linkchecker();
		File metadataFolder = new File(args[0]);
		String username = args[1];
		String password = args[2];
		File brokenFolder = new File(args[3]);

		if (!metadataFolder.exists()) {
			System.out.println("-------------------------------------");
			System.out.println("Error! the folder does not exist-->"
					+ metadataFolder.getAbsolutePath());
			System.out.println("-------------------------------------");
			System.exit(1);
		}
		if (!brokenFolder.exists()) {
			System.out.println("-------------------------------------");
			System.out.println("Error! the folder does not exist--->"
					+ brokenFolder.getAbsolutePath());
			System.out.println("-------------------------------------");
			System.exit(1);
		}
		ods_linkchecker.checkLink(metadataFolder, "linkchecker", "log",
				username, password, brokenFolder);
	}
}
