/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.falcon.regression.core.util;

import com.jcraft.jsch.*;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.falcon.regression.core.bundle.Bundle;
import org.apache.falcon.regression.core.generated.cluster.Cluster;
import org.apache.falcon.regression.core.generated.cluster.Interface;
import org.apache.falcon.regression.core.generated.cluster.Interfacetype;
import org.apache.falcon.regression.core.generated.dependencies.Frequency;
import org.apache.falcon.regression.core.generated.feed.Feed;
import org.apache.falcon.regression.core.generated.feed.Location;
import org.apache.falcon.regression.core.generated.feed.LocationType;
import org.apache.falcon.regression.core.generated.feed.Property;
import org.apache.falcon.regression.core.generated.process.Input;
import org.apache.falcon.regression.core.generated.process.Output;
import org.apache.falcon.regression.core.generated.process.Process;
import org.apache.falcon.regression.core.helpers.ColoHelper;
import org.apache.falcon.regression.core.helpers.PrismHelper;
import org.apache.falcon.regression.core.interfaces.IEntityManagerHelper;
import org.apache.falcon.regression.core.response.APIResult;
import org.apache.falcon.regression.core.response.ProcessInstancesResult;
import org.apache.falcon.regression.core.response.ServiceResponse;
import org.apache.falcon.regression.core.supportClasses.Consumer;
import org.apache.falcon.regression.core.supportClasses.ENTITY_TYPE;
import org.apache.falcon.request.BaseRequest;
import org.apache.falcon.request.RequestKeys;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.authentication.client.AuthenticationException;
import org.apache.http.HttpResponse;
import org.apache.oozie.client.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.testng.Assert;
import org.apache.log4j.Logger;
import org.xml.sax.InputSource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class Util {


    static Logger logger = Logger.getLogger(Util.class);
    static final String MERLIN_PROPERTIES = "Merlin.properties";
    static final String PRISM_PREFIX = "prism";

    static PrismHelper prismHelper = new PrismHelper(MERLIN_PROPERTIES, PRISM_PREFIX);

    public static ServiceResponse sendRequest(String url, String method) throws IOException, URISyntaxException, AuthenticationException{
        return sendRequest(url, method, null, null);
    }

    public static ServiceResponse sendRequest(String url, String method, String user) throws IOException, URISyntaxException, AuthenticationException{
        return sendRequest(url, method, null, user);
    }

    public static ServiceResponse sendRequest(String url, String method, String data,
                                              String user) throws IOException, URISyntaxException, AuthenticationException{
        KerberosHelper.switchUser(user);
        BaseRequest request = new BaseRequest(url, method, user, data);
        request.addHeader(RequestKeys.CONTENT_TYPE_HEADER, RequestKeys.XML_CONTENT_TYPE);
        HttpResponse response = request.run();
        return new ServiceResponse(response);
    }

    public static String getExpectedErrorMessage(String filename) throws IOException {

        Properties properties = new Properties();
        properties.load(Util.class.getResourceAsStream("/" + "errorMapping.properties"));
        return properties.getProperty(filename);
    }

    public static String fileToString(File file) throws IOException {

        StringBuilder fileData = new StringBuilder(1000);
        char[] buf = new char[1024];
        BufferedReader reader = new BufferedReader(
                new FileReader(file));

        int numRead;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        return fileData.toString();
    }

    public static String getProcessName(String data) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(Process.class);
        Unmarshaller u = jc.createUnmarshaller();
        Process processElement = (Process) u.unmarshal((new StringReader(data)));
        return processElement.getName();
    }

    public static String getProcessName(File file) throws IOException, JAXBException {
        return getProcessName(fileToString(file));
    }

    private static boolean isXML(String data) {

        if (data != null && data.trim().length() > 0) {
            if (data.trim().startsWith("<")) {
                return true; //find a better way of validation
            }
        }

        return false;
    }

    public static APIResult parseResponse(ServiceResponse response) throws JAXBException {

        if (!isXML(response.getMessage())) {
            return new APIResult(APIResult.Status.FAILED, response.getMessage(), "somerandomstring",
                    response.getCode());
        }

        JAXBContext jc = JAXBContext.newInstance(APIResult.class);
        Unmarshaller u = jc.createUnmarshaller();
        APIResult temp;
        if (response.getMessage().contains("requestId")) {
            temp = (APIResult) u
                    .unmarshal(new InputSource(new StringReader(response.getMessage())));
            temp.setStatusCode(response.getCode());
        } else {
            temp = new APIResult();
            temp.setStatusCode(response.getCode());
            temp.setMessage(response.getMessage());
            temp.setRequestId("");
            if (response.getCode() == 200) {
                temp.setStatus(APIResult.Status.SUCCEEDED);
            } else {
                temp.setStatus(APIResult.Status.FAILED);
            }
        }

        return temp;
    }

    public static List<String> getProcessStoreInfo(IEntityManagerHelper helper)
    throws IOException, JSchException {
        return getStoreInfo(helper, "/PROCESS");
    }

    public static List<String> getDataSetStoreInfo(IEntityManagerHelper helper)
    throws IOException, JSchException {
        return getStoreInfo(helper, "/FEED");
    }

    public static List<String> getDataSetArchiveInfo(IEntityManagerHelper helper)
    throws IOException, JSchException {
        return getStoreInfo(helper, "/archive/FEED");
    }

    public static List<String> getArchiveStoreInfo(IEntityManagerHelper helper)
    throws IOException, JSchException {
        return getStoreInfo(helper, "/archive/PROCESS");
    }

    public static List<String> getClusterStoreInfo(IEntityManagerHelper helper)
    throws IOException, JSchException {
        return getStoreInfo(helper, "/CLUSTER");
    }

    public static List<String> getClusterArchiveInfo(IEntityManagerHelper helper)
    throws IOException, JSchException {
        return getStoreInfo(helper, "/archive/CLUSTER");
    }

    private static List<String> getStoreInfo(IEntityManagerHelper helper, String subPath)
    throws IOException, JSchException {
        if (helper.getStoreLocation().startsWith("hdfs:")) {
            return HadoopUtil.getAllFilesHDFS(helper.getStoreLocation(),
                    helper.getStoreLocation() + subPath);
        } else {
            return runRemoteScript(helper.getQaHost(), helper.getUsername(),
                    helper.getPassword(), "ls " + helper.getStoreLocation() + "/store" + subPath,
                    helper.getIdentityFile());
        }
    }

    public static List<String> runRemoteScript(String hostName,
                                               String userName,
                                               String password,
                                               String command,
                                               String identityFile)
    throws JSchException, IOException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(userName, hostName, 22);

        System.out.println(
                "host_name: " + hostName + " user_name: " + userName + " password: " + password +
                        " command: " +
                        command);
        // only set the password if its not empty
        if (null != password && !password.isEmpty()) {
            session.setUserInfo(new HardcodedUserInfo(password));
        }
        Properties config = new Properties();
        config.setProperty("StrictHostKeyChecking", "no");
        config.setProperty("UserKnownHostsFile", "/dev/null");
        // only set the password if its not empty
        if (null == password || password.isEmpty()) {
            jsch.addIdentity(identityFile);
        }
        session.setConfig(config);


        session.connect();


        Assert.assertTrue(session.isConnected(), "The session was not connected correctly!");

        ChannelExec channel = (ChannelExec) session.openChannel("exec");


        logger.info("executing the command..." + command);
        channel.setCommand(command);
        channel.setPty(true);
        channel.connect();
        Assert.assertTrue(channel.isConnected(), "The channel was not connected correctly!");
        logger.info("now reading the line....");

        //now to read output
        List<String> data = new ArrayList<String>();

        InputStream in = channel.getInputStream();

        Assert.assertTrue(channel.isConnected(), "The channel was not connected correctly!");

        BufferedReader r = new BufferedReader(new InputStreamReader(in));

        String line;
        while (true) {

            while ((line = r.readLine()) != null) {
                data.add(line);
            }
            if (channel.isClosed()) {

                break;
            }

        }

        in.close();
        r.close();

        channel.disconnect();
        session.disconnect();

        return data;
    }

    public static File[] getFiles(String directoryPath) throws URISyntaxException {
        if (directoryPath.contains("/test-classes"))
            directoryPath = directoryPath.substring(directoryPath.indexOf("/test-classes")
                    + "/test-classes".length() + 1, directoryPath.length());
        System.out.println("directoryPath: " + directoryPath);
        URL url = Util.class.getResource("/" + directoryPath);
        System.out.println("url" + url);
        File dir = new File(url.toURI());
        File[] files = dir.listFiles();
        Arrays.sort(files);
        return files;
    }

    public static String readEntityName(String data) throws JAXBException {

        if (data.contains("uri:falcon:feed"))
            return InstanceUtil.getFeedElement(data).getName();
        else if (data.contains("uri:falcon:process"))
            return InstanceUtil.getProcessElement(data).getName();
        else
            return InstanceUtil.getClusterElement(data).getName();
    }

    public static String readClusterName(String data) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(Cluster.class);
        Unmarshaller u = jc.createUnmarshaller();

        Cluster clusterElement = (Cluster) u.unmarshal(new StringReader(data));

        return clusterElement.getName();
    }

    public static String readDatasetName(String data) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(Feed.class);
        Unmarshaller u = jc.createUnmarshaller();

        Feed datasetElement = (Feed) u.unmarshal((new StringReader(data)));

        return datasetElement.getName();

    }

    public static String generateUniqueProcessEntity(String data) throws JAXBException {

        JAXBContext jc = JAXBContext.newInstance(Process.class);
        Unmarshaller u = jc.createUnmarshaller();
        Process processElement = (Process) u.unmarshal((new StringReader(data)));
        processElement.setName(processElement.getName() + "-" + UUID.randomUUID());
        java.io.StringWriter sw = new StringWriter();
        Marshaller marshaller = jc.createMarshaller();
        marshaller.marshal(processElement, sw);

        return sw.toString();

    }


    public static String generateUniqueClusterEntity(String data) throws JAXBException {

        JAXBContext jc = JAXBContext.newInstance(Cluster.class);
        Unmarshaller u = jc.createUnmarshaller();
        Cluster clusterElement = (Cluster) u.unmarshal((new StringReader(data)));
        clusterElement.setName(clusterElement.getName() + "-" + UUID.randomUUID());

        //lets marshall it back and return
        java.io.StringWriter sw = new StringWriter();
        Marshaller marshaller = jc.createMarshaller();
        marshaller.marshal(clusterElement, sw);

        return sw.toString();
    }

    public static String generateUniqueDataEntity(String data) throws JAXBException {

        JAXBContext jc = JAXBContext.newInstance(Feed.class);
        Unmarshaller u = jc.createUnmarshaller();
        Feed dataElement = (Feed) u.unmarshal((new StringReader(data)));
        dataElement.setName(dataElement.getName() + "-" + UUID.randomUUID());

        return InstanceUtil.feedElementToString(dataElement);
    }

    public static String readPropertiesFile(String filename, String property) {
        return readPropertiesFile(filename, property, null);
    }

    public static String readPropertiesFile(String filename, String property, String defaultValue) {
        String desired_property;

        try {
            InputStream conf_stream = Util.class.getResourceAsStream("/" + filename);

            Properties properties = new Properties();
            properties.load(conf_stream);
            desired_property = properties.getProperty(property, defaultValue);
            conf_stream.close();

            return desired_property;
        } catch (Exception e) {
            logger.info(e.getStackTrace());
        }
        return null;
    }
    public static Bundle[][] readBundles(String path) throws IOException {

        List<Bundle> bundleSet = Util.getDataFromFolder(path);

        Bundle[][] testData = new Bundle[bundleSet.size()][1];

        for (int i = 0; i < bundleSet.size(); i++) {
            testData[i][0] = bundleSet.get(i);
        }

        return testData;
    }

    public static Bundle[][] readBundles() throws IOException {
        return readBundles("bundles");
    }

    public static Bundle[][] readNoOutputBundles() throws IOException {
        return readBundles("ProcessWithNoOutput");
    }

    public static Bundle[][] readELBundles() throws IOException {
        return readBundles("ELbundle");
    }


    public static Bundle[] getBundleData(String path) throws IOException {

        List<Bundle> bundleSet = Util.getDataFromFolder(path);

        return bundleSet.toArray(new Bundle[bundleSet.size()]);
    }

    public static boolean verifyOozieJobStatus(OozieClient client, String processName,
                                               ENTITY_TYPE entityType, Job.Status expectedStatus)
    throws OozieClientException, InterruptedException {
        for (int seconds = 0; seconds < 20; seconds++) {
            Job.Status status = getOozieJobStatus(client, processName, entityType);
            logger.debug("Current status: " + status);
            if (status == expectedStatus) {
                return true;
            }
            TimeUnit.SECONDS.sleep(1);
        }
        return false;
    }

    public static Job.Status getOozieJobStatus(OozieClient client, String processName,
                                               ENTITY_TYPE entityType)
    throws OozieClientException, InterruptedException {
        String filter = String.format("name=FALCON_%s_%s", entityType, processName);
        List<Job.Status> statuses = OozieUtil.getBundleStatuses(client, filter, 0, 10);
        if (statuses.isEmpty()) {
            return null;
        } else {
            return statuses.get(0);
        }
    }

    public static void assertSucceeded(ServiceResponse response) throws JAXBException {
        Assert.assertEquals(Util.parseResponse(response).getStatus(), APIResult.Status.SUCCEEDED);
        Assert.assertEquals(Util.parseResponse(response).getStatusCode(), 200);
        Assert.assertNotNull(Util.parseResponse(response).getMessage());
    }

    public static void assertSucceeded(ServiceResponse response, String message)
    throws JAXBException {
        Assert.assertEquals(Util.parseResponse(response).getStatus(), APIResult.Status.SUCCEEDED,
                message);
        Assert.assertEquals(Util.parseResponse(response).getStatusCode(), 200, message);
    }

    public static void assertPartialSucceeded(ServiceResponse response) throws JAXBException {
        Assert.assertEquals(Util.parseResponse(response).getStatus(), APIResult.Status.PARTIAL);
        Assert.assertEquals(Util.parseResponse(response).getStatusCode(), 400);
        Assert.assertNotNull(Util.parseResponse(response).getMessage());
    }

    public static void assertFailed(ServiceResponse response) throws JAXBException {
        if (response.message.equals("null"))
            Assert.assertTrue(false, "response message should not be null");

        Assert.assertEquals(Util.parseResponse(response).getStatus(), APIResult.Status.FAILED);
        Assert.assertEquals(Util.parseResponse(response).getStatusCode(), 400);
    }

    public static void assertFailed(ServiceResponse response, String message) throws JAXBException {
        if (response.message.equals("null"))
            Assert.assertTrue(false, "response message should not be null");

        Assert.assertEquals(Util.parseResponse(response).getStatus(), APIResult.Status.FAILED,
                message);
        Assert.assertEquals(Util.parseResponse(response).getStatusCode(), 400, message);
        Assert.assertNotNull(Util.parseResponse(response).getRequestId());
    }

    public static void print(String message) {
        logger.info(message);
    }

    public static String getDatasetPath(Bundle bundle) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(Feed.class);

        Unmarshaller u = jc.createUnmarshaller();
        Feed dataElement = (Feed) u.unmarshal((new StringReader(bundle.dataSets.get(0))));
        if (!dataElement.getName().contains("raaw-logs16")) {
            dataElement = (Feed) u.unmarshal(new StringReader(bundle.dataSets.get(1)));
        }

        return dataElement.getLocations().getLocation().get(0).getPath();

    }

    public static List<String> getMissingDependencies(PrismHelper helper, String bundleID)
    throws OozieClientException {
        BundleJob bundleJob = helper.getClusterHelper().getOozieClient().getBundleJobInfo(bundleID);
        CoordinatorJob jobInfo =
                helper.getClusterHelper().getOozieClient().getCoordJobInfo(
                        bundleJob.getCoordinators().get(0).getId());
        List<CoordinatorAction> actions = jobInfo.getActions();

        Util.print("conf from event: " + actions.get(0).getMissingDependencies());

        String[] missingDependencies = actions.get(0).getMissingDependencies().split("#");
        return new ArrayList<String>(Arrays.asList(missingDependencies));
    }

    public static List<String> getCoordinatorJobs(PrismHelper prismHelper, String bundleID)
    throws OozieClientException {
        List<String> jobIds = new ArrayList<String>();
        XOozieClient oozieClient = prismHelper.getClusterHelper().getOozieClient();
        BundleJob bundleJob = oozieClient.getBundleJobInfo(bundleID);
        CoordinatorJob jobInfo =
                oozieClient.getCoordJobInfo(bundleJob.getCoordinators().get(0).getId());

        for (CoordinatorAction action : jobInfo.getActions()) {
            CoordinatorAction actionInfo = oozieClient.getCoordActionInfo(action.getExternalId());
            //  if (actionInfo.getStatus().equals(CoordinatorAction.Status.SUCCEEDED)) {

            jobIds.add(action.getExternalId());
            //  }
        }


        return jobIds;

    }

    public static Date getNominalTime(PrismHelper prismHelper, String bundleID)
    throws OozieClientException {
        XOozieClient oozieClient = prismHelper.getClusterHelper().getOozieClient();
        BundleJob bundleJob = oozieClient.getBundleJobInfo(bundleID);
        CoordinatorJob jobInfo =
                oozieClient.getCoordJobInfo(bundleJob.getCoordinators().get(0).getId());
        List<CoordinatorAction> actions = jobInfo.getActions();

        return actions.get(0).getNominalTime();

    }

    //needs to be rewritten to randomly pick an input feed
    public static String getInputFeedFromBundle(Bundle bundle) throws JAXBException {
        String processData = bundle.getProcessData();

        JAXBContext processContext = JAXBContext.newInstance(Process.class);
        Unmarshaller unmarshaller = processContext.createUnmarshaller();
        Process processObject = (Process) unmarshaller.unmarshal(new StringReader(processData));

        for (Input input : processObject.getInputs().getInput()) {
            for (String feed : bundle.getDataSets()) {
                if (Util.readDatasetName(feed).equalsIgnoreCase(input.getFeed())) {
                    return feed;
                }
            }
        }
        return null;

    }

    public static List<String> getHadoopLateData(ColoHelper helper, String feed)
    throws JAXBException, IOException {
        return getHadoopDataFromDir(helper, feed, "/lateDataTest/testFolders/");
    }

    public static List<String> getHadoopDataFromDir(ColoHelper helper, String feed, String dir)
    throws JAXBException, IOException {
        List<String> finalResult = new ArrayList<String>();

        String feedPath = getFeedPath(feed);
        int depth = feedPath.split(dir)[1].split("/").length - 1;
        List<Path> results = HadoopUtil.getAllDirsRecursivelyHDFS(helper,
                new Path(dir), depth);

        for (Path result : results) {
            int pathDepth = result.toString().split(dir)[1].split("/").length - 1;
            if (pathDepth == depth) {
                finalResult.add(result.toString().split(dir)[1]);
            }
        }

        return finalResult;
    }

    public static List<String> addMinutesToCreatedFolders(List<String> folderList,
                                                          int skipMinutes) {
        logger.info("adding minutes to current folders.....");
        List<String> finalFolderList = new ArrayList<String>();

        if (skipMinutes == 0) {
            skipMinutes = 1;
        }

        for (String date : folderList) {
            for (int i = 0; i < 60; i += skipMinutes) {
                if (i < 10) {
                    finalFolderList.add(date + "/0" + i);
                } else {
                    finalFolderList.add(date + "/" + i);
                }
            }
        }

        return finalFolderList;
    }

    public static List<String> getMinuteDatesOnEitherSide(DateTime startDate, DateTime endDate,
                                                          int minuteSkip) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy/MM/dd/HH/mm");
        logger.info("generating data between " + formatter.print(startDate) + " and " +
                formatter.print(endDate));

        List<String> dates = new ArrayList<String>();


        while (!startDate.isAfter(endDate)) {
            dates.add(formatter.print(startDate.plusMinutes(minuteSkip)));
            if (minuteSkip == 0) {
                minuteSkip = 1;
            }
            startDate = startDate.plusMinutes(minuteSkip);
        }

        return dates;
    }

    public static List<String> getMinuteDatesOnEitherSide(int interval, int minuteSkip) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy/MM/dd/HH/mm");
        if (minuteSkip == 0) {
            minuteSkip = 1;
        }
        DateTime today = new DateTime(DateTimeZone.UTC);
        logger.info("today is: " + today.toString());

        List<String> dates = new ArrayList<String>();
        dates.add(formatter.print(today));

        //first lets get all dates before today
        for (int backward = 1; backward <= interval; backward += minuteSkip) {
            dates.add(formatter.print(today.minusMinutes(backward)));
        }

        //now the forward dates
        for (int i = 0; i <= interval; i += minuteSkip) {
            dates.add(formatter.print(today.plusMinutes(i)));
        }

        return dates;
    }

    public static List<String> getMinuteDatesOnEitherSide(int interval, int daySkip,
                                                          int minuteSkip) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy/MM/dd/HH");
        DateTime today = new DateTime(DateTimeZone.UTC);
        logger.info("today is: " + today.toString());

        List<String> dates = new ArrayList<String>();
        dates.add(formatter.print(today));

        //first lets get all dates before today
        for (int backward = 1; backward <= interval; backward += daySkip + 1) {
            dates.add(formatter.print(today.minusDays(backward)));
        }

        //now the forward dates
        for (int i = 1; i <= interval; i += daySkip + 1) {
            dates.add(formatter.print(today.plusDays(i)));
        }

        return addMinutesToCreatedFolders(dates, minuteSkip);
    }

    public static String setFeedProperty(String feed, String propertyName, String propertyValue)
    throws JAXBException {

        Feed feedObject = InstanceUtil.getFeedElement(feed);

        boolean found = false;
        for (Property prop : feedObject.getProperties().getProperty()) {
            //check if it is present
            if (prop.getName().equalsIgnoreCase(propertyName)) {
                prop.setValue(propertyValue);
                found = true;
                break;
            }
        }

        if (!found) {
            Property property = new Property();
            property.setName(propertyName);
            property.setValue(propertyValue);
            feedObject.getProperties().getProperty().add(property);
        }


        return InstanceUtil.feedElementToString(feedObject);

    }

    public static String getFeedPath(String feed) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(Feed.class);
        Unmarshaller um = context.createUnmarshaller();
        Feed feedObject = (Feed) um.unmarshal(new StringReader(feed));

        for (Location location : feedObject.getLocations().getLocation()) {
            if (location.getType().equals(LocationType.DATA)) {
                return location.getPath();
            }
        }

        return null;
    }

    public static ExecResult executeCommand(String command) {
        Util.print("Command to be executed: " + command);
        StringBuilder errors = new StringBuilder();
        StringBuilder output = new StringBuilder();

        try {
            java.lang.Process process = Runtime.getRuntime().exec(command);

            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = errorReader.readLine()) != null) {
                errors.append(line + "\n");
            }

            while ((line = consoleReader.readLine()) != null) {
                output.append(line + "\n");
            }
            final int exitVal = process.waitFor();
            logger.info("exitVal: " + exitVal);
            logger.info("output: " + output);
            logger.info("errors: " + errors);
            return new ExecResult(exitVal, output.toString().trim(), errors.toString().trim());
        } catch (InterruptedException e) {
            Assert.fail("Process execution failed:" + ExceptionUtils.getStackTrace(e));
        } catch (IOException e) {
            Assert.fail("Process execution failed:" + ExceptionUtils.getStackTrace(e));
        }
        return null;
    }

    public static int executeCommandGetExitCode(String command) {
        return executeCommand(command).getExitVal();
    }

    public static String executeCommandGetOutput(String command) throws IOException, InterruptedException {
        return executeCommand(command).getOutput();
    }

    public static String insertLateFeedValue(String feed, String delay, String delayUnit)
    throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(Feed.class);
        Unmarshaller um = context.createUnmarshaller();
        Feed feedObject = (Feed) um.unmarshal(new StringReader(feed));


        String delayTime = "";

        if (delayUnit.equalsIgnoreCase("hours")) {
            delayTime = "hours(" + delay + ")";
        } else if (delayUnit.equalsIgnoreCase("minutes")) {
            delayTime = "minutes(" + delay + ")";
        } else if (delayUnit.equalsIgnoreCase("days")) {
            delayTime = "days(" + delay + ")";
        } else if (delayUnit.equalsIgnoreCase("months")) {
            delayTime = "months(" + delay + ")";
        }

        feedObject.getLateArrival().setCutOff(new Frequency(delayTime));

        Marshaller m = context.createMarshaller();
        StringWriter sw = new StringWriter();

        m.marshal(feedObject, sw);

        return sw.toString();
    }

    public static void createLateDataFolders(PrismHelper prismHelper, List<String> folderList)
    throws IOException, InterruptedException {
        logger.info("creating late data folders.....");
        Configuration conf = new Configuration();
        conf.set("fs.default.name", "hdfs://" + prismHelper.getProcessHelper().getHadoopURL() + "");

        final FileSystem fs = FileSystem.get(conf);

        folderList.add("somethingRandom");

        for (final String folder : folderList) {
            fs.mkdirs(new Path("/lateDataTest/testFolders/" + folder));
        }

        logger.info("created all late data folders.....");
    }

    public static void copyDataToFolders(PrismHelper prismHelper, List<String> folderList,
                                         String directory)
    throws IOException, InterruptedException {
        logger.info("copying data into folders....");

        Configuration conf = new Configuration();
        conf.set("fs.default.name", "hdfs://" + prismHelper.getClusterHelper().getHadoopURL() + "");

        final FileSystem fs = FileSystem.get(conf);

        for (final String folder : folderList) {
            File[] dirFiles = new File(directory).listFiles();
            assert dirFiles != null;
            for (final File file : dirFiles) {
                if (!file.isDirectory()) {
                    fs.copyFromLocalFile(new Path(file.getAbsolutePath()),
                            new Path("/lateDataTest/testFolders/" + folder));
                }
            }
        }

        logger.info("copied data into latedata folders....");
    }

    public static DateTime getSystemDate(PrismHelper prismHelper)
    throws IOException, JSchException {

        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy'-'MM'-'dd'T'HH':'mm'Z'");


        return fmt.parseDateTime(runRemoteScript(prismHelper.getClusterHelper()
                .getQaHost(), prismHelper.getClusterHelper().getUsername(),
                prismHelper.getClusterHelper().getPassword(), "date '+%Y-%m-%dT%H:%MZ'",
                prismHelper.getClusterHelper().getIdentityFile()).get(0));

    }

    public static List<String> getBundles(OozieClient client, String entityName,
                                          ENTITY_TYPE entityType)
    throws OozieClientException {
        String filter = "name=FALCON_" + entityType + "_" + entityName;
        return OozieUtil.getBundleIds(client, filter, 0, 10);
    }

    public static String setFeedPathValue(String feed, String pathValue) throws JAXBException {
        JAXBContext feedContext = JAXBContext.newInstance(Feed.class);
        Feed feedObject = (Feed) feedContext.createUnmarshaller().unmarshal(new StringReader(feed));

        //set the value
        for (Location location : feedObject.getLocations().getLocation()) {
            if (location.getType().equals(LocationType.DATA)) {
                location.setPath(pathValue);
            }
        }

        StringWriter feedWriter = new StringWriter();
        feedContext.createMarshaller().marshal(feedObject, feedWriter);
        return feedWriter.toString();
    }

    public static List<DateTime> getStartTimeForRunningCoordinators(PrismHelper prismHelper,
                                                                    String bundleID)
    throws OozieClientException {
        List<DateTime> startTimes = new ArrayList<DateTime>();

        XOozieClient oozieClient = prismHelper.getClusterHelper().getOozieClient();
        BundleJob bundleJob = oozieClient.getBundleJobInfo(bundleID);
        CoordinatorJob jobInfo;


        for (CoordinatorJob job : bundleJob.getCoordinators()) {

            if (job.getAppName().contains("DEFAULT")) {
                jobInfo = oozieClient.getCoordJobInfo(job.getId());
                for (CoordinatorAction action : jobInfo.getActions()) {
                    DateTime temp = new DateTime(action.getCreatedTime(), DateTimeZone.UTC);
                    logger.info(temp);
                    startTimes.add(temp);
                }
            }

            Collections.sort(startTimes);

            if (!(startTimes.isEmpty())) {
                return startTimes;
            }
        }

        return null;
    }

    public static String getBundleStatus(PrismHelper prismHelper, String bundleId)
    throws OozieClientException {
        XOozieClient oozieClient = prismHelper.getClusterHelper().getOozieClient();
        BundleJob bundleJob = oozieClient.getBundleJobInfo(bundleId);
        return bundleJob.getStatus().toString();
    }

    public static String findFolderBetweenGivenTimeStamps(DateTime startTime, DateTime endTime,
                                                          List<String> folderList) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy/MM/dd/HH/mm");

        for (String folder : folderList) {
            if (folder.compareTo(formatter.print(startTime)) >= 0 &&
                    folder.compareTo(formatter.print(endTime)) <= 0) {
                return folder;
            }
        }
        return null;
    }

    @Deprecated
    public static void HDFSCleanup(PrismHelper prismHelper, String hdfsPath) throws IOException {
        Configuration conf = new Configuration();
        conf.set("fs.default.name", "hdfs://" + prismHelper.getProcessHelper().getHadoopURL());
        final FileSystem fs = FileSystem.get(conf);
        HDFSCleanup(fs, hdfsPath);
    }

    public static void HDFSCleanup(FileSystem fs, String hdfsPath) throws IOException {
        HadoopUtil.deleteDirIfExists(hdfsPath, fs);
    }

    public static void lateDataReplenish(PrismHelper prismHelper, int interval,
                                         int minuteSkip) throws IOException, InterruptedException {
        List<String> folderData = Util.getMinuteDatesOnEitherSide(interval, minuteSkip);

        Util.createLateDataFolders(prismHelper, folderData);
        Util.copyDataToFolders(prismHelper, folderData,
                "src/test/resources/OozieExampleInputData/normalInput");
    }

    public static void lateDataReplenish(PrismHelper prismHelper, String baseFolder, int interval,
                                         int minuteSkip, String... files)
    throws IOException, InterruptedException {
        List<String> folderData = Util.getMinuteDatesOnEitherSide(interval, minuteSkip);

        Util.createLateDataFolders(prismHelper, folderData);
        Util.copyDataToFolders(prismHelper, baseFolder, folderData, files);
    }

    public static void injectMoreData(PrismHelper prismHelper, final String remoteLocation,
                                      String localLocation)
    throws IOException, InterruptedException {
        Configuration conf = new Configuration();
        conf.set("fs.default.name", "hdfs://" + prismHelper.getClusterHelper().getHadoopURL() + "");

        final FileSystem fs = FileSystem.get(conf);

        File[] files = new File(localLocation).listFiles();
        assert files != null;
        for (final File file : files) {
            if (!file.isDirectory()) {
                String path = "/lateDataTest/testFolders/" + remoteLocation + "/" +
                        System.currentTimeMillis() / 1000 + "/";
                System.out.println("inserting data@ " + path);
                fs.copyFromLocalFile(new Path(file.getAbsolutePath()), new Path(path));
            }
        }

    }

    public static String getInputFeedNameFromBundle(Bundle b) throws JAXBException {
        String feedData = getInputFeedFromBundle(b);

        JAXBContext processContext = JAXBContext.newInstance(Feed.class);
        Unmarshaller unmarshaller = processContext.createUnmarshaller();
        Feed feedObject = (Feed) unmarshaller.unmarshal(new StringReader(feedData));

        return feedObject.getName();
    }

    public static void lateDataReplenish(PrismHelper prismHelper, int interval,
                                         int minuteSkip,
                                         String folderPrefix)
    throws IOException, InterruptedException {
        List<String> folderPaths = Util.getMinuteDatesOnEitherSide(interval, minuteSkip);
        Util.print("folderData: " + folderPaths.toString());

        Util.createLateDataFolders(prismHelper, folderPaths, folderPrefix);
        Util.copyDataToFolders(prismHelper, folderPrefix, folderPaths,
                "src/test/resources/OozieExampleInputData/normalInput/_SUCCESS",
                "src/test/resources/OozieExampleInputData/normalInput/log_01.txt");
    }

    public static void createLateDataFolders(PrismHelper prismHelper, List<String> folderList,
                                             final String FolderPrefix)
    throws IOException, InterruptedException {
        Configuration conf = new Configuration();
        conf.set("fs.default.name", "hdfs://" + prismHelper.getProcessHelper().getHadoopURL() + "");

        final FileSystem fs = FileSystem.get(conf);

        for (final String folder : folderList) {
            fs.mkdirs(new Path(FolderPrefix + folder));
        }
    }

    public static void copyDataToFolders(PrismHelper prismHelper, final String folderPrefix,
                                         List<String> folderList,
                                         String... fileLocations)
    throws IOException, InterruptedException {
        Configuration conf = new Configuration();
        conf.set("fs.default.name", "hdfs://" + prismHelper.getProcessHelper().getHadoopURL());

        final FileSystem fs = FileSystem.get(conf);

        for (final String folder : folderList) {
            boolean r;
            String folder_space = folder.replaceAll("/", "_");
            File f = new File(
                    "src/test/resources/OozieExampleInputData/normalInput/" + folder_space +
                            ".txt");
            if (!f.exists()) {
                r = f.createNewFile();
                if (!r)
                    System.out.println("file could not be created");
            }


            FileWriter fr = new FileWriter(f);
            fr.append("folder");
            fr.close();
            fs.copyFromLocalFile(new Path(f.getAbsolutePath()), new Path(folderPrefix + folder));
            r = f.delete();
            if (!r)
                System.out.println("delete was not successful");


            for (final String file : fileLocations) {
                logger.info("copying  " + file + " to " + folderPrefix + folder);
                fs.copyFromLocalFile(new Path(file), new Path(folderPrefix + folder));
            }
        }
    }

    public static String getFeedName(String feedData) throws JAXBException {
        JAXBContext processContext = JAXBContext.newInstance(Feed.class);
        Unmarshaller unmarshaller = processContext.createUnmarshaller();
        Feed feedObject = (Feed) unmarshaller.unmarshal(new StringReader(feedData));
        return feedObject.getName();
    }

    public static String getOutputFeedFromBundle(Bundle bundle) throws JAXBException {
        String processData = bundle.getProcessData();

        JAXBContext processContext = JAXBContext.newInstance(Process.class);
        Unmarshaller unmarshaller = processContext.createUnmarshaller();
        Process processObject = (Process) unmarshaller.unmarshal(new StringReader(processData));

        for (Output output : processObject.getOutputs().getOutput()) {
            for (String feed : bundle.getDataSets()) {
                if (Util.readDatasetName(feed).equalsIgnoreCase(output.getFeed())) {
                    return feed;
                }
            }
        }
        return null;
    }

    public static String setFeedName(String outputFeed, String newName) throws JAXBException {
        JAXBContext feedContext = JAXBContext.newInstance(Feed.class);
        Feed feedObject =
                (Feed) feedContext.createUnmarshaller().unmarshal(new StringReader(outputFeed));

        //set the value
        feedObject.setName(newName);
        StringWriter feedWriter = new StringWriter();
        feedContext.createMarshaller().marshal(feedObject, feedWriter);
        return feedWriter.toString().trim();
    }

    public static CoordinatorJob getDefaultOozieCoord(PrismHelper prismHelper, String bundleId,
                                                      ENTITY_TYPE type)
    throws OozieClientException {
        XOozieClient client = prismHelper.getClusterHelper().getOozieClient();
        BundleJob bundlejob = client.getBundleJobInfo(bundleId);

        for (CoordinatorJob coord : bundlejob.getCoordinators()) {
            if ((coord.getAppName().contains("DEFAULT") && ENTITY_TYPE.PROCESS
                    .equals(type)) || (coord.getAppName().contains("REPLICATION") && ENTITY_TYPE
                    .FEED
                    .equals(type))) {
                return client.getCoordJobInfo(coord.getId());
            } else {
                System.out.println("Desired coord does not exists");
            }
        }

        return null;
    }

    public static Cluster getClusterObject(
            String clusterXML) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(Cluster.class);
        Unmarshaller um = context.createUnmarshaller();
        return (Cluster) um.unmarshal(new StringReader(clusterXML));
    }

    public static List<String> getInstanceFinishTimes(ColoHelper coloHelper, String workflowId)
    throws IOException, JSchException {
        List<String> raw = runRemoteScript(coloHelper.getProcessHelper()
                .getQaHost(), coloHelper.getProcessHelper().getUsername(),
                coloHelper.getProcessHelper().getPassword(),
                "cat /var/log/ivory/application.* | grep \"" + workflowId + "\" | grep " +
                        "\"Received\" | awk '{print $2}'",
                coloHelper.getProcessHelper().getIdentityFile());
        List<String> finalList = new ArrayList<String>();
        for (String line : raw) {
            finalList.add(line.split(",")[0]);

        }

        return finalList;
    }

    public static List<String> getInstanceRetryTimes(ColoHelper coloHelper, String workflowId)
    throws IOException, JSchException {
        List<String> raw = runRemoteScript(coloHelper.getProcessHelper()
                .getQaHost(), coloHelper.getProcessHelper().getUsername(),
                coloHelper.getProcessHelper().getPassword(),
                "cat /var/log/ivory/application.* | grep \"" + workflowId + "\" | grep " +
                        "\"Retrying attempt\" | awk '{print $2}'",
                coloHelper.getProcessHelper().getIdentityFile());
        List<String> finalList = new ArrayList<String>();
        for (String line : raw) {
            finalList.add(line.split(",")[0]);
        }

        return finalList;
    }

    public static void shutDownService(IEntityManagerHelper helper)
    throws IOException, JSchException {
        runRemoteScriptAsSudo(helper.getQaHost(), helper.getUsername(),
                helper.getPassword(), helper.getServiceStopCmd(),
                helper.getServiceUser(), helper.getIdentityFile());
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
    }

    public static void startService(IEntityManagerHelper helper) throws IOException, JSchException {
        runRemoteScriptAsSudo(helper.getQaHost(), helper.getUsername(),
                helper.getPassword(), helper.getServiceStartCmd(), helper.getServiceUser(),
                helper.getIdentityFile());
        int statusCode = 0;
        for (int tries = 20; tries > 0; tries--) {
            try {
                statusCode = Util.sendRequest(helper.getHostname(), "get").getCode();
            } catch (IOException e) {
            } catch (URISyntaxException e) {
            } catch (AuthenticationException e) {
            }
            if (statusCode == 200) return;
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        }
        throw new RuntimeException("Service on" + helper.getHostname() + " did not start!");
    }

    public static void restartService(IEntityManagerHelper helper)
    throws IOException, JSchException {
        Util.print("restarting service for: " + helper.getQaHost());

        shutDownService(helper);
        startService(helper);
    }

    private static List<String> runRemoteScriptAsSudo(String hostName,
                                                      String userName,
                                                      String password,
                                                      String command,
                                                      String
                                                              runAs,
                                                      String identityFile
    ) throws JSchException, IOException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(userName, hostName, 22);
        // only set the password if its not empty
        if (null != password && !password.isEmpty()) {
            session.setUserInfo(new HardcodedUserInfo(password));
        }
        Properties config = new Properties();
        config.setProperty("StrictHostKeyChecking", "no");
        config.setProperty("UserKnownHostsFile", "/dev/null");
        // only set the password if its not empty
        if (null == password || password.isEmpty()) {
            jsch.addIdentity(identityFile);
        }
        session.setConfig(config);
        session.connect();
        Assert.assertTrue(session.isConnected(), "The session was not connected correctly!");

        List<String> data = new ArrayList<String>();

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setPty(true);
        String runCmd;
        if (null == runAs || runAs.isEmpty()) {
            runCmd = "sudo -S -p '' " + command;
        } else {
            runCmd = String.format("sudo su - %s -c '%s'", runAs, command);
        }
        System.out.println(
                "host_name: " + hostName + " user_name: " + userName + " password: " + password +
                        " command: " +
                        runCmd);
        channel.setCommand(runCmd);
        InputStream in = channel.getInputStream();
        OutputStream out = channel.getOutputStream();
        channel.setErrStream(System.err);

        channel.connect();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
        // only print the password if its not empty
        if (null != password && !password.isEmpty()) {
            out.write((password + "\n").getBytes());
            out.flush();
        }

        in.close();
        channel.disconnect();
        session.disconnect();
        out.close();
        return data;
    }

    public static Process getProcessObject(String processData) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(Process.class);
        Unmarshaller um = context.createUnmarshaller();
        return (Process) um.unmarshal(new StringReader(processData));
    }

    public static Feed getFeedObject(String feedData) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(Feed.class);
        Unmarshaller um = context.createUnmarshaller();

        return (Feed) um.unmarshal(new StringReader(feedData));


    }

    public static int getNumberOfWorkflowInstances(PrismHelper prismHelper, String bundleId)
    throws OozieClientException {
        return getDefaultOozieCoord(prismHelper, bundleId,
                ENTITY_TYPE.PROCESS).getActions().size();
    }

    public static List<String> getActionsNominalTime(PrismHelper prismHelper,
                                                     String bundleId,
                                                     ENTITY_TYPE type)
    throws OozieClientException {
        List<String> nominalTime = new ArrayList<String>();
        List<CoordinatorAction> actions = getDefaultOozieCoord(prismHelper,
                bundleId, type).getActions();
        for (CoordinatorAction action : actions) {
            nominalTime.add(action.getNominalTime().toString());
        }
        return nominalTime;
    }

    public static List<String> generateUniqueClusterEntity(List<String> clusterData)
    throws JAXBException {
        List<String> newList = new ArrayList<String>();
        for (String cluster : clusterData) {
            newList.add(generateUniqueClusterEntity(cluster));
        }

        return newList;
    }

    public static void dumpConsumerData(Consumer consumer) {
        logger.info("dumping all queue data:");

        for (HashMap<String, String> data : consumer.getMessageData()) {
            logger.info("*************************************");
            for (String key : data.keySet()) {
                logger.info(key + "=" + data.get(key));
            }
            logger.info("*************************************");
        }
    }

    public static void lateDataReplenish(PrismHelper prismHelper, int interval,
                                         int minuteSkip,
                                         String folderPrefix, String postFix)
    throws IOException, InterruptedException {
        List<String> folderPaths = Util.getMinuteDatesOnEitherSide(interval, minuteSkip);
        Util.print("folderData: " + folderPaths.toString());

        if (postFix != null) {
            for (int i = 0; i < folderPaths.size(); i++)
                folderPaths.set(i, folderPaths.get(i) + postFix);
        }

        Util.createLateDataFolders(prismHelper, folderPaths, folderPrefix);
        Util.copyDataToFolders(prismHelper, folderPrefix, folderPaths,
                "src/test/resources/OozieExampleInputData/normalInput/_SUCCESS",
                "src/test/resources/OozieExampleInputData/normalInput/log_01.txt");
    }

    public static void assertSucceeded(ProcessInstancesResult response) {
        Assert.assertNotNull(response.getMessage());
        Assert.assertTrue(
                response.getMessage().contains("SUCCEEDED") ||
                        response.getStatus().toString().equals("SUCCEEDED"));
    }

    public static void assertFailed(ProcessInstancesResult response) {
        Assert.assertNotNull(response.getMessage());
        Assert.assertTrue(response.getMessage().contains("FAILED") ||
                response.getStatus().toString().equals("FAILED"));
    }

    public static boolean isBundleOver(ColoHelper coloHelper, String bundleId)
    throws OozieClientException {
        XOozieClient client = coloHelper.getClusterHelper().getOozieClient();

        BundleJob bundleJob = client.getBundleJobInfo(bundleId);

        if (bundleJob.getStatus().equals(BundleJob.Status.DONEWITHERROR) ||
                bundleJob.getStatus().equals(BundleJob.Status.FAILED) ||
                bundleJob.getStatus().equals(BundleJob.Status.SUCCEEDED) ||
                bundleJob.getStatus().equals(BundleJob.Status.KILLED)) {
            return true;
        }


        try {
            TimeUnit.SECONDS.sleep(20);
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
        return false;
    }

    public static void lateDataReplenishWithout_Success(PrismHelper prismHelper, int interval,
                                                        int minuteSkip, String folderPrefix,
                                                        String postFix)
    throws IOException, InterruptedException {
        List<String> folderPaths = Util.getMinuteDatesOnEitherSide(interval, minuteSkip);
        Util.print("folderData: " + folderPaths.toString());

        if (postFix != null) {
            for (int i = 0; i < folderPaths.size(); i++)
                folderPaths.set(i, folderPaths.get(i) + postFix);
        }

        Util.createLateDataFolders(prismHelper, folderPaths, folderPrefix);
        Util.copyDataToFolders(prismHelper, folderPrefix, folderPaths,
                "src/test/resources/OozieExampleInputData/normalInput/log_01.txt");
    }

    public static void putFileInFolderHDFS(PrismHelper prismHelper, int interval, int minuteSkip,
                                           String folderPrefix, String fileToBePut)
    throws IOException, InterruptedException {
        List<String> folderPaths = Util.getMinuteDatesOnEitherSide(interval, minuteSkip);
        Util.print("folderData: " + folderPaths.toString());

        Util.createLateDataFolders(prismHelper, folderPaths, folderPrefix);

        if (fileToBePut.equals("_SUCCESS"))
            Util.copyDataToFolders(prismHelper, folderPrefix, folderPaths,
                    "src/test/resources/OozieExampleInputData/normalInput/_SUCCESS");

        else
            Util.copyDataToFolders(prismHelper, folderPrefix, folderPaths,
                    "src/test/resources/OozieExampleInputData/normalInput/log_01.txt");

    }

    public static void submitAllClusters(Bundle... b)
    throws IOException, URISyntaxException, AuthenticationException {
        for (Bundle aB : b) {
            Util.print("Submitting Cluster: " + aB.getClusters().get(0));
            ServiceResponse r = prismHelper.getClusterHelper()
                    .submitEntity(URLS.SUBMIT_URL, aB.getClusters().get(0));
            Assert.assertTrue(r.getMessage().contains("SUCCEEDED"));

        }
    }

    public static Properties getPropertiesObj(String filename) {
        try {
            Properties properties = new Properties();

            System.out.println("filename: " + filename);
            InputStream conf_stream =
                    Util.class.getResourceAsStream("/" + filename);
            properties.load(conf_stream);
            conf_stream.close();
            return properties;

        } catch (Exception e) {
            logger.info(e.getStackTrace());
        }
        return null;
    }

    public static List<Bundle> getDataFromFolder(String folderPath) throws IOException {

        List<Bundle> bundleList = new ArrayList<Bundle>();
        File[] files;
        try {
            files = Util.getFiles(folderPath);
        } catch (URISyntaxException e) {
            return bundleList;
        }


        List<String> dataSets = new ArrayList<String>();
        String processData = new String();
        String clusterData = new String();

        for (int i = 0; i < files.length; i++) {

            if (files[i].getName().contains("svn")
                    || files[i].getName().contains(".DS")
                    || files[i].getName() == null) {
                continue;
            } else {
                if (files[i].isDirectory()) {
                    bundleList.addAll(getDataFromFolder(new String(files[i]
                            .getAbsolutePath())));
                } else {

                    String data = fileToString(new File(files[i].getAbsolutePath()));

                    if (data.contains("uri:ivory:process:0.1") ||
                            data.contains("uri:falcon:process:0.1")) {
                        System.out.println("data been added to process: " + data);
                        processData = data;
                    } else if (data.contains("uri:ivory:cluster:0.1") ||
                            data.contains("uri:falcon:cluster:0.1")) {
                        System.out.println("data been added to cluster: " + data);
                        clusterData = data;
                    } else if (data.contains("uri:ivory:feed:0.1") ||
                            data.contains("uri:falcon:feed:0.1")) {
                        System.out.println("data been added to feed: " + data);
                        dataSets.add(data);
                    }
                }
            }

        }
        if (!(dataSets.isEmpty()) && processData != ""
                && !"".equals(clusterData)) {
            bundleList.add(new Bundle(dataSets, processData, clusterData));
        } else if (processData != ""
                && !"".equals(clusterData))
            bundleList.add(new Bundle(dataSets, processData, clusterData));

        return bundleList;

    }

    public static String getEnvClusterXML(String filename, String cluster, String prefix)
    throws JAXBException {

        Cluster clusterObject =
                getClusterObject(cluster);
        if ((null == prefix) || prefix.isEmpty())
            prefix = "";
        else prefix = prefix + ".";

        //now read and set relevant values
        for (Interface iface : clusterObject.getInterfaces().getInterface()) {
            if (iface.getType().equals(Interfacetype.READONLY)) {
                iface.setEndpoint(readPropertiesFile(filename, prefix + "cluster_readonly"));
            } else if (iface.getType().equals(Interfacetype.WRITE)) {
                iface.setEndpoint(readPropertiesFile(filename, prefix + "cluster_write"));
            } else if (iface.getType().equals(Interfacetype.EXECUTE)) {
                iface.setEndpoint(readPropertiesFile(filename, prefix + "cluster_execute"));
            } else if (iface.getType().equals(Interfacetype.WORKFLOW)) {
                iface.setEndpoint(readPropertiesFile(filename, prefix + "oozie_url"));
            } else if (iface.getType().equals(Interfacetype.MESSAGING)) {
                iface.setEndpoint(readPropertiesFile(filename, prefix + "activemq_url"));
            }
        }

        //set colo name:
        clusterObject.setColo(readPropertiesFile(filename, prefix + "colo"));

        // get the properties object for the cluster
        org.apache.falcon.regression.core.generated.cluster.Properties clusterProperties =
                clusterObject.getProperties();
        // create the property object for the namenode princpal
        org.apache.falcon.regression.core.generated.cluster.Property namenodePrincipal = new org
                .apache.falcon.regression.core.generated.cluster.Property();
        namenodePrincipal.setName("dfs.namenode.kerberos.principal");
        namenodePrincipal
                .setValue(readPropertiesFile(filename, prefix + "namenode.kerberos.principal", "none"));
        // add the namenode principal to the properties object
        clusterProperties.getProperty().add(namenodePrincipal);

        // create the property for the hive meta store principal
        org.apache.falcon.regression.core.generated.cluster.Property hivePrincipal = new org
                .apache.falcon.regression.core.generated.cluster.Property();
        hivePrincipal.setName("hive.metastore.kerberos.principal");
        hivePrincipal.setValue(readPropertiesFile(filename, prefix + "hive.metastore.kerberos" +
                ".principal", "none"));
        // add the hive meta store principal to the properties object
        clusterProperties.getProperty().add(hivePrincipal);

        JAXBContext context = JAXBContext.newInstance(Cluster.class);
        Marshaller m = context.createMarshaller();
        StringWriter writer = new StringWriter();

        m.marshal(clusterObject, writer);
        return writer.toString();
    }

    public static void validateNumberOfWorkflowInstances(PrismHelper prismHelper, int originalCount,
                                                         String oldBundleId, String updatedBundleId)
    throws Exception {
        //first make sure sum of all parts is same
        Assert.assertEquals(getNumberOfWorkflowInstances(prismHelper, oldBundleId) +
                getNumberOfWorkflowInstances(prismHelper, updatedBundleId), originalCount,
                "The total number of workflow instances dont match post update! Please check.");

    }

    public static void verifyNewBundleCreation(ColoHelper coloHelper,
                                               String originalBundleId,
                                               int originalBundleCount,
                                               String processName, boolean shouldBeCreated)
    throws Exception {
        String newBundleId =
                InstanceUtil.getLatestBundleID(coloHelper, processName, ENTITY_TYPE.PROCESS);
        if (shouldBeCreated) {
            Assert.assertTrue(!newBundleId.equalsIgnoreCase(originalBundleId),
                    "eeks! new bundle is not getting created!!!!");
            System.out.println("old bundleId=" + originalBundleId);
            System.out.println("new bundleId=" + newBundleId);
            Util.validateNumberOfWorkflowInstances(coloHelper,
                    originalBundleCount, originalBundleId, newBundleId);
        } else {
            Assert.assertEquals(newBundleId,
                    originalBundleId, "eeks! new bundle is getting created!!!!");
        }
    }

    public static void verifyNewBundleCreation(ColoHelper cluster,
                                               String originalBundleId,
                                               List<String>
                                                       initialNominalTimes,
                                               String processName,
                                               boolean shouldBeCreated,
                                               ENTITY_TYPE type) throws
    Exception {

        String newBundleId =
                InstanceUtil.getLatestBundleID(cluster, processName, ENTITY_TYPE.PROCESS);
        if (shouldBeCreated) {
            Assert.assertTrue(!newBundleId.equalsIgnoreCase(originalBundleId),
                    "eeks! new bundle is not getting created!!!!");
            System.out.println("old bundleId=" + originalBundleId);
            System.out.println("new bundleId=" + newBundleId);
            Util.validateNumberOfWorkflowInstances(cluster,
                    initialNominalTimes, originalBundleId, newBundleId, type);
        } else {
            Assert.assertEquals(newBundleId,
                    originalBundleId, "eeks! new bundle is getting created!!!!");
        }
    }

    private static void validateNumberOfWorkflowInstances(ColoHelper cluster,
                                                          List<String> initialNominalTimes,
                                                          String originalBundleId,
                                                          String newBundleId, ENTITY_TYPE type)
    throws OozieClientException {

        List<String> nominalTimesOriginalAndNew = Util.getActionsNominalTime
                (cluster,
                        originalBundleId, type);

        nominalTimesOriginalAndNew.addAll(Util.getActionsNominalTime(cluster,
                newBundleId, type));

        initialNominalTimes.removeAll(nominalTimesOriginalAndNew);

        if (initialNominalTimes.size() != 0)
            Assert.assertFalse(true, "some instances have gone missing after " +
                    "update");
    }

    public enum URLS {

        SUBMIT_URL("/api/entities/submit"),
        GET_ENTITY_DEFINITION("/api/entities/definition"),
        DELETE_URL("/api/entities/delete"),
        SCHEDULE_URL("/api/entities/schedule"),
        VALIDATE_URL("/api/entities/validate"),
        SUSPEND_URL("/api/entities/suspend"),
        RESUME_URL("/api/entities/resume"),
        STATUS_URL("/api/entities/status"),
        SUBMIT_AND_SCHEDULE_URL("/api/entities/submitAndSchedule"),
        INSTANCE_RUNNING("/api/instance/running"),
        INSTANCE_STATUS("/api/instance/status"),
        INSTANCE_KILL("/api/instance/kill"),
        INSTANCE_RESUME("/api/instance/resume"),
        INSTANCE_SUSPEND("/api/instance/suspend"),
        PROCESS_UPDATE("/api/entities/update/process"),
        INSTANCE_RERUN("/api/instance/rerun"),
        FEED_UPDATE("/api/entities/update/feed");
        private final String url;

        URLS(String url) {
            this.url = url;
        }

        public String getValue() {
            return this.url;
        }
    }

    private static class HardcodedUserInfo implements UserInfo {

        private final String password;

        private HardcodedUserInfo(String password) {
            this.password = password;
        }

        public String getPassphrase() {
            return null;
        }

        public String getPassword() {
            return password;
        }

        public boolean promptPassword(String s) {
            return true;
        }

        public boolean promptPassphrase(String s) {
            return true;
        }

        public boolean promptYesNo(String s) {
            return true;
        }

        public void showMessage(String s) {
            logger.info("message = " + s);
        }
    }

    public static Bundle getBundle(ColoHelper cluster, String... xmlLocation) {
        Bundle b;
        try {
            if (xmlLocation.length == 1)
                b = (Bundle) Bundle.readBundle(xmlLocation[0])[0][0];
            else if (xmlLocation.length == 0)
                b = (Bundle) Util.readELBundles()[0][0];
            else {
                System.out.println("invalid size of xmlLocaltions return null");
                return null;
            }

            b.generateUniqueBundle();
            return new Bundle(b, cluster.getEnvFileName(), cluster.getPrefix());
        } catch (Exception e) {
            System.out.println(Arrays.toString(e.getStackTrace()));
        }
        return null;
    }

    public static String getMethodType(String url) {
        List<String> postList = new ArrayList<String>();
        postList.add("/entities/validate");
        postList.add("/entities/submit");
        postList.add("/entities/submitAndSchedule");
        postList.add("/entities/suspend");
        postList.add("/entities/resume");
        postList.add("/instance/kill");
        postList.add("/instance/suspend");
        postList.add("/instance/resume");
        postList.add("/instance/rerun");
        for (String item : postList) {
            if (url.toLowerCase().contains(item)) {
                return "post";
            }
        }
        List<String> deleteList = new ArrayList<String>();
        deleteList.add("/entities/delete");
        for (String item : deleteList) {
            if (url.toLowerCase().contains(item)) {
                return "delete";
            }
        }

        return "get";
    }

    public static String formatXml(String xmlString) {
        try {
            Source xmlInput = new StreamSource(new StringReader(xmlString));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", "2");
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(xmlInput, xmlOutput);
            return xmlOutput.getWriter().toString();
        } catch (TransformerConfigurationException e) {
            return xmlString;
        } catch (TransformerException e) {
            return xmlString;
        }

    }
}
