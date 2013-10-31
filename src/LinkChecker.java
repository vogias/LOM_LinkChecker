/**
 * Created by IntelliJ IDEA.
 * User: Enayat Rajabi
 * Date: 21/02/2013
 * Version: 1.0
 * Time: 5:59 PM
 * To change this template use File | Settings | File Templates.
 */

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.sql.*;


public class LinkChecker extends JFrame implements ActionListener {


    JButton button,button2, checkButton;
    JTextField field, databaseNameField, tableNameField, brokenFilesPath;
    JTextField mysqlUserName, mysqlPasswd, RepositoryField;
    JLabel  mysqlUserNameLabel,mysqlPasswdLabel,RepositoryFieldLabel ;
    JLabel label, processing, pathLabel, databaseName,tableName,brokenFilesPathLabel;
    JMenuBar bar;
    JMenu fileMenu;
    int fileNumber=0;

    final JDialog d = new JDialog();
    JPanel p1 = new JPanel(new GridBagLayout());
    int deadLinks=0,liveLinks=0,notWellFormed=0;
    public void insertRecordToliveLinks(String URL,String Result, String FileName, Connection conn, String tableName){
        Statement stmt = null;
        FileName = FileName.replace('\\','/');
        try{
            stmt = conn.createStatement();
            String sql = "INSERT INTO "+tableName+" (URL,Result,FileName)" +
                    "VALUES ('"+ URL+"','"+ Result+"','"+ FileName+"');";
            System.out.print(sql);
            stmt.executeUpdate(sql);
        }catch(SQLException se){
            se.printStackTrace();
        }catch(Exception e){
            //Handle errors for Class.forName
            e.printStackTrace();
        }finally{
        }//end try
    }//end main

    // ------------     Get Connection from MySQL       --------------------------
    public Connection Get_Connection(String databaseName) throws Exception
    {
        Connection connection = null;
        try
        {
            if(mysqlUserName.getText().isEmpty()|| mysqlPasswd.getText().isEmpty() ){
                processing.setVisible(false);
                JOptionPane.showMessageDialog(this, "Database username or password can not be empty! Try again.");
                System.out.println(" --------------------Error: Database username or password can not be empty! -----------------------------");
                System.exit(0);
            }
            else{
                String userName=mysqlUserName.getText();
                String passWord=mysqlPasswd.getText();

                System.out.println("Connecting to a selected database...");
                //  Name of database
                String connectionURL = "jdbc:mysql://localhost:3306/"+databaseName;

                Class.forName("com.mysql.jdbc.Driver").newInstance();

                //  Connection username and password
                connection = DriverManager.getConnection(connectionURL, userName,passWord);
                System.out.println("Connected database successfully...");
                return connection;
            }
        }
        catch (SQLException e)
        {
            System.out.println("SQL problem...");
            throw e;
        }
        catch (Exception e)
        {
            System.out.println("Connection problem...");
            throw e;
        }
        return connection;
    }

    // ------------     INSERT RECORD INTO LOG TABLE   (URL, Dead or Live?, the filename, connection name)  --------------------------
    public void insertRecord(String URL,String Result, String FileName, Connection conn, String tbName){
        Statement stmt = null;
        //String tableName="log";
        FileName = FileName.replace('\\','/');
        try{
            stmt = conn.createStatement();
            String sql = "INSERT INTO "+tbName+" (URL,Result,FileName)" +
                    "VALUES ('"+ URL+"','"+ Result+"','"+ FileName+"');";
            System.out.print(sql);
            stmt.executeUpdate(sql);
        }catch(SQLException se){
            //Handle errors for JDBC
            se.printStackTrace();
        }catch(Exception e){
            //Handle errors for Class.forName
            e.printStackTrace();
        }finally{
        }//end try
    }

    // ------------     INSERT Aggregation RECORD INTO aggregation TABLE     --------------------------
    public void insertRecordtoAggregation(String FolderName,int liveLinks,int deadLinks, int totalFiles, int NotWell){
        Connection conn = null;
        Statement stmt = null;
        String tableName="aggregation";
        FolderName = FolderName.replace('\\','/');
        try{
            // System.out.println("Inserting records into the table...");
            conn=Get_Connection("odslinkchecker");
            stmt = conn.createStatement();

            String sql = "INSERT INTO "+tableName+" (Folder,liveLinks,deadLinks,totalFiles,NotWellFormed)" +
                    "VALUES ('"+ FolderName+"','"+ liveLinks+"','"+ deadLinks+"','"+ totalFiles+"','"+NotWell+"');";
            System.out.print(sql);
            stmt.executeUpdate(sql);

        }catch(SQLException se){
            //Handle errors for JDBC
            se.printStackTrace();
        }catch(Exception e){
            //Handle errors for Class.forName
            e.printStackTrace();
        }finally{
            //finally block used to close resources
            try{
                if(stmt!=null)
                    conn.close();
            }catch(SQLException se){
            }// do nothing
            try{
                if(conn!=null)
                    conn.close();
            }catch(SQLException se){
                se.printStackTrace();
            }//end finally try
        }//end try
    }

    // ------------    Delete All the table  (Dangerous!)          --------------------------
    public void deleteAll(){
        Connection conn = null;
        Statement stmt = null;
        try{
            System.out.println("deleting the table...");
            conn=Get_Connection("linkcheckerlog");
            stmt = conn.createStatement();

            String sql = "DELETE FROM log;";
            System.out.println(sql);
            stmt.executeUpdate(sql);
            System.out.println("all deleted from log");
            sql = "DELETE FROM aggregation;";
            System.out.println(sql);
            stmt.executeUpdate(sql);
            System.out.println("all deleted from aggregation");

        }catch(SQLException se){
            //Handle errors for JDBC
            se.printStackTrace();
        }catch(Exception e){
            //Handle errors for Class.forName
            e.printStackTrace();
        }finally{
            //finally block used to close resources
            try{
                if(stmt!=null)
                    conn.close();
            }catch(SQLException se){
            }// do nothing
            try{
                if(conn!=null)
                    conn.close();
            }catch(SQLException se){
                se.printStackTrace();
            }//end finally try
        }//end try
    }//end main

    // ------------     OPEN XML FILE  AND READ AN ELEMENT THAT IS UNDER ROOT->ELEMENT            --------------------------
    String GetURLFromXML(String fileName,String root, String element) {
        String urlCheck=null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            File file = new File(fileName);
            if (file.exists()) {
                Document doc = db.parse(file);
                Element docEle = doc.getDocumentElement();

                // Print root element of the document
                //System.out.println("Root element of the document: "
                //       + docEle.getNodeName());

                try{
                    // Root element --- here is general ---
                    NodeList ElementList = docEle.getElementsByTagName(root);
                    if (ElementList != null && ElementList.getLength() > 0) {
                        for (int i = 0; i < ElementList.getLength(); i++) {
                            Node node = ElementList.item(i);
                            if (node.getNodeType() == Node.ELEMENT_NODE) {
                                // System.out.println("=====================");
                                Element e = (Element) node;
                                try{
                                    // Link element= identifier. entry
                                    NodeList nodeList = e.getElementsByTagName(element);
                                    urlCheck=nodeList.item(0).getChildNodes().item(0).getNodeValue();
                                }catch (Exception ee){
                                    urlCheck="Error";
                                }
                            }
                        }
                    } else {
                        //System.exit(1);
                        urlCheck="Error";
                    }
                }catch(Exception eg){
                    urlCheck="Error";
                }
            }
        } catch (Exception e) {
            //System.out.println(e);
            urlCheck="Error";
        }
        return urlCheck;
    }

    // ---------------  Read a Text file with many URLs and check the broken links of them----------------
    private void checkDeadLinksFromTxtFile(File folderName){
        String filename= folderName.listFiles()[0].getPath();
        int Dead=0;
        Connection conn=null;
        try{
            conn=Get_Connection("globe_linkchecker");
        }catch(SQLException se){
            //Handle errors for JDBC
            se.printStackTrace();
        }catch(Exception e){
            //Handle errors for Class.forName
            e.printStackTrace();
        }

        try{
            // Open the file that is the first
            // command line parameter
            FileInputStream fstream = new FileInputStream(filename);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String filepath, url;
            //Read File Line By Line
            int counter=0;
            while ((filepath = br.readLine()) != null)   {
                url = br.readLine();
                // Print the content on the console
                System.out.println("URL number:"+ (++counter));
                if (!isLive(url))
                    try {
                        // if the URL is dead so insert a record into the table with Dead value
                        insertRecord(url, "Dead", filepath, conn,"log");
                        Dead+=1;
                    }
                    catch (Exception e) {}
                // you can limit your record to 1000 URLs
                //if (counter==1000)
                //    break;
            }
            //Close the input stream
            in.close();
        }catch (Exception e){//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }

        //--- CLOSE the CONNECTION
        try{
            if(conn!=null)
                conn.close();
        }catch(SQLException se){
            //Handle errors for JDBC
            se.printStackTrace();
        }
        System.out.println("Finished! Total dead URLs:"+ (Dead+1));
    }

    // ------------     CHECK THE URL, IF IT'S OK RETURN TRUE      --------------------------
    private static boolean isLive(String link){

        HttpURLConnection urlconn = null;

        int res = -1;
        String msg = null;
        try{

            URL url = new URL(link);

            String protocol = url.getProtocol();
            System.out.println(url);
            // If the URL is https, I assumed it is live
            if(protocol.equals("https")) {
                //HttpsURLConnection hc = (HttpsURLConnection)url.openConnection();
               // hc.setConnectTimeout(5000);
               // hc.setReadTimeout(5000);
               // hc.disconnect();
                return true;
            }

            urlconn = (HttpURLConnection)url.openConnection();
            urlconn.setConnectTimeout(10000);
            urlconn.setReadTimeout(10000);
            urlconn.setRequestMethod("GET");

            urlconn.connect();
           String redirlink = urlconn.getHeaderField("Location");
           System.out.println(urlconn.getHeaderFields());

            if(redirlink != null && !url.toExternalForm().equals(redirlink)){
                       return isLive(redirlink);
            }
            else
                return urlconn.getResponseCode()==HttpURLConnection.HTTP_OK;
        }catch(Exception e){

            System.out.println(e.getMessage());
            return false;

        }finally{

            if(urlconn != null)
                urlconn.disconnect();
        }

    }

    private static int URLChecker(String link){

        HttpURLConnection urlconn = null;

        int res = -1;
        String msg = null;
        try{

            URL url = new URL(link);

            String protocol = url.getProtocol();
            // System.out.println(url);
            if(!protocol.equals("http")) {
                //HttpsURLConnection hc = (HttpsURLConnection)url.openConnection();
                // hc.setConnectTimeout(5000);
                // hc.setReadTimeout(5000);
                // hc.disconnect();
                return -2;
            }

            urlconn = (HttpURLConnection)url.openConnection();
            urlconn.setConnectTimeout(10000);
            urlconn.setReadTimeout(10000);
            urlconn.setRequestMethod("GET");

            urlconn.connect();
            String redirlink = urlconn.getHeaderField("Location");
            // System.out.println(urlconn.getHeaderFields());

            if(redirlink != null && !url.toExternalForm().equals(redirlink)){
                return URLChecker(redirlink);
            }
            else
                return urlconn.getResponseCode();

        }catch(Exception e){
            System.out.println(e.getMessage());
            if(e.getMessage().startsWith("time"))
                return 1000;
            if(e.getMessage().startsWith("Connection refused"))
                return 2000;
            else
                return -1;
        }finally{

            if(urlconn != null)
                urlconn.disconnect();
        }

    }
      // ---- This is a method checks that is it a valid URL
   /* private static boolean isURLValid(String url){
        String[] schemes = {"ftps","ftp","http","https"};
        UrlValidator urlValidator = new UrlValidator(schemes);
        if (urlValidator.isValid(url)) {
            return true;
        } else {
            return false;
        }
    }   */

    // ---------------  This is main link checker, open a folder, read XML files         ----------------
    //----------------- read an element and checks its availability  ------------------
    void checkLink(File folder, String dbName, String tbName) {
        File[] listOfFiles;
        String fileName=null;
        Connection conn = null;
        int recordsNumber=0;

        try{
            conn=Get_Connection(dbName);
        }catch(SQLException se){
            //Handle errors for JDBC
            se.printStackTrace();
        }catch(Exception e){
            //Handle errors for Class.forName
            e.printStackTrace();
        }

        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".xml") || name.endsWith(".XML");
            }
        };

        try{
            listOfFiles = folder.listFiles(filter);
            fileNumber=listOfFiles.length;
            if(fileNumber==0){
                JOptionPane.showMessageDialog(this, "No XML files found in the selected folder");
                processing.setVisible(false);
            }else{
                // processing.setText("processing "+fileNumber+" files ...");
                processing.setVisible(true);
                System.out.println("processing "+fileNumber+" files ...");
                //JOptionPane.showMessageDialog(this, "Folder Name hi="+listOfFiles.length);
                deadLinks=0;
                liveLinks=0;
                notWellFormed=0;
                for (int i = 0; i < fileNumber; i++) {
                    System.out.println("processing "+(i+1)+"th file ...");
                    fileName=listOfFiles[i].getPath().toString();
                    System.out.println("FileName= "+fileName);
                    String metadataURL=(GetURLFromXML(fileName,"technical","location").toString());
                    if(metadataURL.equals("Error"))
                        metadataURL=(GetURLFromXML(fileName,"lom:technical","lom:location").toString());
                    if(metadataURL.equals("Error"))
                    {
                        //  JOptionPane.showMessageDialog(this, fileName+" is not well-formed! General.Entry does not exist.");
                        try {
                          /*  BufferedWriter bw = new BufferedWriter(new FileWriter(new File(logFile), true));
                       // Date date = new Date();
                        //SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a");
                        //String formattedDate = sdf.format(date);
                        //String text= "FileName="+fileName+" URL="+metadataURL;//+"Time="+formattedDate;
                        bw.write(fileName);
                        bw.newLine();
                        bw.write(metadataURL);
                        bw.newLine();
                        bw.close();
                       //if(metadataURL.equals("Error")){
                       //else insertRecord(metadataURL,"NotWellFormed",fileName);    */
                            System.out.println("--------------------File #("+i+")-----------------------------");
                            insertRecord(metadataURL,"NotWellFormed",fileName,conn, tbName);
                            System.out.println("--------------------File #("+i+")-----------------------------");

                            notWellFormed+=1;
                            //}
                        }
                        catch (Exception e) {
                            JOptionPane.showMessageDialog(this, " Can not connect to the database");
                            System.exit(2);
                        }
                    } else
                    {
                        /*BufferedWriter bw = new BufferedWriter(new FileWriter(new File(logFile), true));
                        bw.write(fileName);
                        bw.newLine();
                        bw.write(metadataURL);
                        bw.newLine();
                        bw.close();  */
                        //String resultText=null;
                        int result=URLChecker(metadataURL);
                        if(result!=200){
                             try {
                                 System.out.println("--------------------File #("+i+")-----------------------------");
                                 insertRecord(metadataURL,Integer.toString(result),fileName,conn, tbName);
                                 System.out.println("----------------------------------------------------");
                                 //insertRecordToliveLinks(metadataURL,"live",fileName,conn,"livelinks");
                                 deadLinks+=1;
                             } catch (Exception e) {

                             }
                        } else{
                            try {
                                //insertRecordToliveLinks(metadataURL,"dead",fileName,conn,"deadlinks");
                                System.out.println("--------------------File #("+i+")-----------------------------");
                                insertRecord(metadataURL,Integer.toString(result),fileName,conn,tbName);
                                System.out.println("--------------------------------------------------------");
                                liveLinks+=1;
                            } catch (Exception e) {

                            }
                        }
                    }
                    recordsNumber=i;
                }
                d.setVisible(false);
                try{
                    if(conn!=null)
                        conn.close();
                }catch(SQLException se){
                    //Handle errors for JDBC
                    se.printStackTrace();
                }
                //for MACE filenumber is 50000 so the filenumber is 50000+notWellFormed
                //insertRecordtoAggregation(folder.toString(),liveLinks,deadLinks,fileNumber,notWellFormed);
                JOptionPane.showMessageDialog(this, "Saved to DB! Dead links="+deadLinks+" ** Not Well-Formed="+notWellFormed);
                System.out.println(" --------------------Finsished! -----------------------------");
                System.out.println(" Total number of checked records="+recordsNumber);
                System.out.println(" Number of broken links="+deadLinks);
                System.out.println(" Number of not well-formed="+notWellFormed);
                System.out.println(" ------------------------------------------------------------");


                //processing.setVisible(false);
            }
        }catch(Exception NotFolder){
            JOptionPane.showMessageDialog(this, "Un-expected Error ");
            //throw NotFolder;
        }

    }

    // ---------------- Move the borken XML files to another folder --------------------------------------------

    void moveDeadFilestoAnotherFolderbyFileName(File folder, String dbName,String tableName, String newFolder) {
        Connection conn = null;
        Statement stmt = null;
        int fileNumber=0;
        String Repository=null;
       if(RepositoryField.getText().isEmpty()){
           JOptionPane.showMessageDialog(this, "Repository is empty! Try again.");
           System.out.println(" --------------------Error: Repository is empty! -----------------------------");
           return;
       }
       else
          Repository=RepositoryField.getText();

        try{
            conn=Get_Connection(dbName);
        }catch(SQLException se){
            //Handle errors for JDBC
            se.printStackTrace();
        }catch(Exception e){
            //Handle errors for Class.forName
            e.printStackTrace();
        }
        try{
            stmt = conn.createStatement();
            //String sql = "SELECT filename from "+tableName+" where FileName like '%/"+Repository+"/%' and (Result=404 or Result=403 or Result like 'NotWellFormed');";
            String sql = "SELECT filename from "+tableName+" where FileName like '%/"+Repository+"/%' and Result=200;";

            System.out.println("SQL="+sql);
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                File source=new File(rs.getString("FileName"));
                File dest= new File(newFolder+"\\"+source.getName());
                System.out.println("File_source=" + source);
                System.out.println("File_dest=" + dest);
                System.out.println("Exists="+dest.exists());
                //----------------------------------------------
                if(source.exists()){
                    Files.move(source.toPath(), dest.toPath());
                    System.out.println("Moving= " + (++fileNumber) + "th ");
                }
                //-----------------------------------------------
           }
            rs.close();
        }catch(Exception ee){}
        try{
            if(conn!=null) {
                conn.close();
                stmt.close();
            }
        }catch(SQLException se){
            se.printStackTrace();
        }
        System.out.println("total= "+ fileNumber);

    }

    // ---------------  Class  Frame (interface)       ----------------
    public LinkChecker () {
        this.setLayout(null);

        button = new JButton("browse folder");
        button2 = new JButton("browse folder");

        field = new JTextField();
        databaseNameField=new JTextField("linkcheckerlog");
        tableNameField=new JTextField("log");
        brokenFilesPath=new JTextField();
        mysqlUserName= new JTextField("root");
        mysqlPasswd=new JTextField("123");
        RepositoryField=new JTextField("COSMOS");


        checkButton =new JButton("check links") ;
        label= new JLabel("Link Checker - V0.9 2013-06");
        pathLabel= new JLabel("Folder path:");
        databaseName= new JLabel("Database Name:");
        tableName= new JLabel("Table Name:");
        brokenFilesPathLabel=new JLabel("Broken File Path:");
        mysqlUserNameLabel=new JLabel("DB User:");
        mysqlPasswdLabel=new JLabel("DB Password:");
        RepositoryFieldLabel=new JLabel("Repository:");


        processing= new JLabel("Processing...");
        p1.add(new JLabel("Processing, Please Wait..."),new GridBagConstraints());
        d.getContentPane().add(p1);
        d.setSize(300,100);
        d.setLocationRelativeTo(this);
        d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        //------ MENU BAR ---------------------
        bar = new JMenuBar();
        fileMenu = new JMenu("Action");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        bar.add(fileMenu);
        JMenuItem eMenuItem = new JMenuItem("DeleteAll");
        eMenuItem.setMnemonic(KeyEvent.VK_C);
        fileMenu.add(eMenuItem);

        field.setBounds(100, 50, 300, 25);
        button.setBounds(400, 50, 120, 25);
        button2.setBounds(400, 130, 120, 25);

        label.setBounds(100,-30,500,100);
        pathLabel.setBounds(30, 50, 100, 25);
        databaseName.setBounds(5, 75, 100, 25);
        databaseNameField.setBounds(100, 75, 200, 25);
        mysqlUserNameLabel.setBounds(320,75,50,25);
        mysqlUserName.setBounds(400,75,100,25);

        tableName.setBounds(25, 100, 100, 25);
        tableNameField.setBounds(100, 100, 200, 25);
        mysqlPasswdLabel.setBounds(320,100,100,25);
        mysqlPasswd.setBounds(400,100,100,25);

        brokenFilesPathLabel.setBounds(5, 130, 200, 25);
        brokenFilesPath.setBounds(100, 130, 300, 25);

        RepositoryFieldLabel.setBounds(35, 160, 200, 25);
        RepositoryField.setBounds(100, 160, 100, 25);

        checkButton.setBounds(100,200,100,20);
        processing.setBounds(35, 100, 500, 100);

        this.setJMenuBar(bar);
        this.add(field);
        this.add(button);
        this.add(button2);

        this.add(label);
        this.add(checkButton);
        this.add(processing);
        this.add(pathLabel);
        this.add(databaseNameField);
        this.add(databaseName);
        this.add(tableNameField);
        this.add(tableName);
        this.add(brokenFilesPath);
        this.add(brokenFilesPathLabel);
        this.add(mysqlUserName);
        this.add(mysqlUserNameLabel);
        this.add(mysqlPasswd);
        this.add(mysqlPasswdLabel);
        this.add(RepositoryField);
        this.add(RepositoryFieldLabel);

        processing.setVisible(false);

        button.addActionListener(this);
        button2.addActionListener(this);

        checkButton.addActionListener(this);

        eMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if(okcancel("Are you sure to delete all the records?")==0){
                    deleteAll();
                    showMessage("All the records were deleted");
                }
            }

        });

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
    }

    //  ---- when user clicks on check link and browses folder
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == button) {
            Chooser frame = new Chooser();
            field.setText(frame.folderName);
        }
        if (e.getSource() == button2) {
            Chooser frame = new Chooser();
            brokenFilesPath.setText(frame.folderName);
        }
        if (e.getSource() == checkButton) {
            File folder = new File(field.getText());
            if(!folder.exists()){
                JOptionPane.showMessageDialog(this, "Choose a valid folder");
                System.out.println("This is not valid folder");
                //throw NotFolder;
            }else{
                d.setModal(true);
                SwingWorker<?,?> worker = new SwingWorker<Void,Integer>(){
                    public Void doInBackground() throws InterruptedException{
                        // Thread.sleep(10);
                        File folder = new File(field.getText());
                        String databaseName = databaseNameField.getText();
                        String tableName = tableNameField.getText();
                        String brokenPath=brokenFilesPath.getText();

                        checkLink(folder, databaseName,tableName);
                        //moveDeadFilestoAnotherFolderbyFileName(folder,databaseName,tableName,brokenPath);
                        return null;
                    }

                    protected void done(){
                        d.dispose();
                    }
                };
                worker.execute();
                d.setVisible(true);
            }
        }
    }

    //  ---- Show OK_Cancel message
    public static int okcancel(String theMessage) {
        int result = JOptionPane.showConfirmDialog((Component) null, theMessage,
                "alert", JOptionPane.OK_CANCEL_OPTION);
        return result;
    }

    //  ---- Show message
    public static void  showMessage(String theMessage) {
        JOptionPane.showMessageDialog((Component) null, theMessage,
                "Message", JOptionPane.INFORMATION_MESSAGE);
    }
    public static void main(String args[]) {
        LinkChecker frame = new LinkChecker ();

        frame.setSize(700, 300);
        frame.setLocation(200, 100);
        frame.setVisible(true);
        frame.setTitle("XML-LOM Link checker (UAH)");

    }
}

class Chooser extends JFrame {

    JFileChooser chooser;
    String fileName,folderName;

    public Chooser() {
        String[] files={};
        chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        JDialog DialogS=new JDialog();
        int r = chooser.showOpenDialog(new JFrame());
        if (r == JFileChooser.APPROVE_OPTION) {
            folderName = chooser.getSelectedFile().getPath();
        }

    }
}

