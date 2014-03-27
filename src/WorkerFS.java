import java.io.File;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;

/**
 * 
 */

/**
 * @author vogias
 * 
 */
public class WorkerFS implements Runnable {

	String provider;
	File inputFile;
	lom_linkchecker ods_linkchecker;

	File brokenFolder;
	Logger slf4jLogger;
	String code = "null";

	public WorkerFS(String provider, File inputFile,
			lom_linkchecker ods_linkchecker, File brokenFolder,
			Logger slf4jLogger) {
		// TODO Auto-generated constructor stub
		this.provider = provider;
		this.inputFile = inputFile;
		this.ods_linkchecker = ods_linkchecker;
		this.brokenFolder = brokenFolder;
		this.slf4jLogger = slf4jLogger;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		StringBuffer logString = new StringBuffer();

		logString.append(provider);

		System.out.println("processing file:" + inputFile.getName());
		String fileName = inputFile.getPath().toString();
		String name = inputFile.getName();
		name = name.substring(0, name.indexOf(".xml"));

		logString.append(" " + name);

		System.out.println("checking FileName= " + fileName);
		String metadataURL = (ods_linkchecker.GetURLFromXML(fileName,
				"technical", "location").toString()).replace("'", "\\'");
		if (metadataURL.equals("Error"))
			metadataURL = (ods_linkchecker.GetURLFromXML(fileName,
					"lom:technical", "lom:location").toString());
		if (metadataURL.equals("Error"))
			metadataURL = (ods_linkchecker.GetURLFromXML(fileName, "oai_dc:dc",
					"dc:identifier").toString());

		if (metadataURL.equals("Error")) {
			try {
				System.out.println("--------------------File #("
						+ inputFile.getName()
						+ ")-----------------------------");
				// ods_linkchecker.insertRecord(metadataURL, "NotWellFormed",
				// fileName, conn, tbName);
				code = "NotWellFormed";

				logString.append(" " + "NotWellFormed");
				logString.append(" " + code);

				System.out.println("--------------------File #("
						+ inputFile.getName()
						+ ")-----------------------------");
				System.out.println("No metadata element found!");

				ods_linkchecker.raiseNotWellFormed();

				// -------------------------Move files
				// ----------------------------------
				// File newFile =new
				// File(brokenFolder.toPath()+"\\"+listOfFiles[i].getName());
				File newFile = new File(brokenFolder.getPath(),
						inputFile.getName());

				Files.move(inputFile.toPath(), newFile.toPath(),StandardCopyOption.REPLACE_EXISTING);
				
				
				System.out.println("File=" + inputFile.toPath()
						+ " was moved to" + brokenFolder);
				// ----------------------------------------------------------------------
			} catch (Exception e) {
				System.out
						.println("Error in moving file in NotWellFormed section");
				System.exit(2);
			}
		} else {
			System.out.println("Found URL=" + metadataURL);

			int result = ods_linkchecker.URLChecker(metadataURL);
			if (result != 200) {
				try {
					System.out.println("--------------------File #("
							+ inputFile.getName()
							+ ")-----------------------------");
					// ods_linkchecker.insertRecord(metadataURL,
					// Integer.toString(result), fileName, conn, tbName);

					code = String.valueOf(result);
					System.out
							.println("----------------------------------------------------");
					ods_linkchecker.raiseDeadLinks();

					// -------------------------Move files
					// ----------------------------------
					System.out.println("File source=" + inputFile.toPath());
					System.out.println("File Distination="
							+ brokenFolder.toPath());

					// File newFile =new
					// File(brokenFolder.toPath()+"\\"+listOfFiles[i].getName());
					File newFile = new File(brokenFolder.getPath(),
							inputFile.getName());
					Files.move(inputFile.toPath(), newFile.toPath(),StandardCopyOption.REPLACE_EXISTING);
					System.out.println("File=" + inputFile.toPath()
							+ " was moved to" + brokenFolder);
					logString.append(" " + "DeadLink");
					logString.append(" " + code);
					// ----------------------------------------------------------------------

				} catch (Exception e) {
					System.out
							.println("Error in moving file in result<>200 section");
				}
			} else {
				try {
					System.out.println("--------------------File #("
							+ inputFile.getName()
							+ ")-----------------------------");
					// ods_linkchecker.insertRecord(metadataURL,
					// Integer.toString(result), fileName, conn, tbName);
					code = String.valueOf(result);
					System.out
							.println("--------------------------------------------------------");
					ods_linkchecker.raiseLiveLinks();

					logString.append(" " + "Livelink");
					logString.append(" " + code);
				} catch (Exception e) {
					System.out
							.println("Error in inserting record  in result=200 section");
				}
			}
		}
		ods_linkchecker.raiseRecordsNumber();

		slf4jLogger.info(logString.toString());
	}

}
