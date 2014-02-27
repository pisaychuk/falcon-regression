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

package org.apache.falcon.regression.prism;

import org.apache.falcon.regression.core.bundle.Bundle;
import org.apache.falcon.regression.core.helpers.ColoHelper;
import org.apache.falcon.regression.core.supportClasses.ENTITY_TYPE;
import org.apache.falcon.regression.core.util.HadoopUtil;
import org.apache.falcon.regression.core.util.InstanceUtil;
import org.apache.falcon.regression.core.util.Util;
import org.apache.falcon.regression.testHelper.BaseTestClass;
import org.apache.hadoop.fs.FileSystem;
import org.apache.oozie.client.CoordinatorAction;
import org.apache.oozie.client.OozieClient;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import java.io.IOException;
import java.lang.reflect.Method;


@Test(groups = "embedded")
public class OptionalInputTest extends BaseTestClass {

    ColoHelper cluster;
    FileSystem clusterFS;
    OozieClient oozieClient;
    String baseTestDir = baseHDFSDir + "/OptionalInputTest";
    String inputPath = baseTestDir + "/input";
    String aggregateWorkflowDir = baseWorkflowDir + "/aggregator";
    Bundle b = new Bundle();

    public OptionalInputTest() throws IOException {
        super();
        cluster = servers.get(1);
        oozieClient = serverOC.get(1);
        clusterFS = serverFS.get(1);
    }

    @BeforeClass
    public void uploadWorkflow() throws Exception {
        HadoopUtil.uploadDir(clusterFS, aggregateWorkflowDir, "src/test/resources/oozie");
    }

    @BeforeMethod(alwaysRun = true)
    public void setup(Method method) throws Exception {
        Util.print("test name: " + method.getName());
        b = Util.readELBundles()[0][0];
        b = new Bundle(b, cluster.getEnvFileName(), cluster.getPrefix());
        b.setProcessWorkflow(aggregateWorkflowDir);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
        b.deleteBundle(prism);
        HadoopUtil.deleteDirIfExists(inputPath + "/", clusterFS);
    }

    @Test(enabled = true, groups = {"singleCluster"})
    public void optionalTest_1optional_1compulsary() throws Exception {

        //process with 2 input , scheduled on single cluster
        // in input set true / false for both the input
        //create data after process has been scheduled, so that initially instance goes into waiting
        b = b.getRequiredBundle(b, 1, 2, 1, inputPath, 1, "2010-01-02T01:00Z",
                "2010-01-02T01:12Z");

        for (int i = 0; i < b.getClusters().size(); i++)
            Util.print(b.getDataSets().get(i));

        for (int i = 0; i < b.getDataSets().size(); i++)
            Util.print(b.getDataSets().get(i));

        Util.print(b.getProcessData());

        b.submitAndScheduleBundle(b, prism, false);

        Thread.sleep(20000);

        InstanceUtil.createDataWithinDatesAndPrefix(cluster,
                InstanceUtil.oozieDateToDate("2010-01-02T00:00Z"),
                InstanceUtil.oozieDateToDate("2010-01-02T01:15Z"), inputPath + "/input1/",
                1);

        InstanceUtil
                .waitTillInstanceReachState(oozieClient, Util.getProcessName(b.getProcessData()),
                        2, CoordinatorAction.Status.SUCCEEDED, 20, ENTITY_TYPE.PROCESS);
    }

    @Test(enabled = true, groups = {"singleCluster"})
    public void optionalTest_1optional_2compulsary() throws Exception {
        //process with 3 input , scheduled on single cluster
        // in input set true / false for both the input
        //create data after process has been scheduled, so that initially instance goes into waiting
        b = b.getRequiredBundle(b, 1, 3, 1, inputPath, 1, "2010-01-02T01:00Z",
                "2010-01-02T01:12Z");

        for (int i = 0; i < b.getClusters().size(); i++)
            Util.print(b.getDataSets().get(i));

        for (int i = 0; i < b.getDataSets().size(); i++)
            Util.print(b.getDataSets().get(i));

        Util.print(b.getProcessData());

        b.submitAndScheduleBundle(b, prism, false);

        Thread.sleep(20000);


        Util.print("instanceShouldStillBeInWaitingState");
        InstanceUtil
                .waitTillInstanceReachState(oozieClient, Util.getProcessName(b.getProcessData()),
                        2, CoordinatorAction.Status.WAITING, 5, ENTITY_TYPE.PROCESS);

        InstanceUtil.createDataWithinDatesAndPrefix(cluster,
                InstanceUtil.oozieDateToDate("2010-01-01T22:00Z"),
                InstanceUtil.oozieDateToDate("2010-01-02T03:00Z"), inputPath + "/input2/",
                1);
        InstanceUtil.createDataWithinDatesAndPrefix(cluster,
                InstanceUtil.oozieDateToDate("2010-01-01T22:00Z"),
                InstanceUtil.oozieDateToDate("2010-01-02T03:00Z"), inputPath + "/input1/",
                1);

        InstanceUtil
                .waitTillInstanceReachState(oozieClient, Util.getProcessName(b.getProcessData()),
                        2, CoordinatorAction.Status.SUCCEEDED, 20, ENTITY_TYPE.PROCESS);
    }

    @Test(enabled = true, groups = {"singleCluster"})
    public void optionalTest_2optional_1compulsary() throws Exception {

        //process with 2 input , scheduled on single cluster
        // in input set true / false for both the input
        //create data after process has been scheduled, so that initially instance goes into waiting
        b = b.getRequiredBundle(b, 1, 3, 2, inputPath, 1, "2010-01-02T01:00Z",
                "2010-01-02T01:12Z");

        for (int i = 0; i < b.getClusters().size(); i++)
            Util.print(b.getDataSets().get(i));

        for (int i = 0; i < b.getDataSets().size(); i++)
            Util.print(b.getDataSets().get(i));

        Util.print(b.getProcessData());

        b.submitAndScheduleBundle(b, prism, false);

        Thread.sleep(20000);
        InstanceUtil
                .waitTillInstanceReachState(oozieClient, Util.getProcessName(b.getProcessData()),
                        2, CoordinatorAction.Status.WAITING, 3, ENTITY_TYPE.PROCESS);

        InstanceUtil.createDataWithinDatesAndPrefix(cluster,
                InstanceUtil.oozieDateToDate("2010-01-01T22:00Z"),
                InstanceUtil.oozieDateToDate("2010-01-02T04:00Z"), inputPath + "/input2/",
                1);

        InstanceUtil
                .waitTillInstanceReachState(oozieClient, Util.getProcessName(b.getProcessData()),
                        2, CoordinatorAction.Status.SUCCEEDED, 20, ENTITY_TYPE.PROCESS);
    }


    @Test(enabled = true, groups = {"singleCluster"})
    public void optionalTest_optionalInputWithEmptyDir() throws Exception {

        //process with 2 input , scheduled on single cluster
        // in input set true / false for both the input
        //create data after process has been scheduled, so that initially instance goes into waiting
        String startTime = InstanceUtil.getTimeWrtSystemTime(-4);
        String endTime = InstanceUtil.getTimeWrtSystemTime(10);

        // b = (Bundle)Util.readBundles("src/test/resources/updateBundle")[0][0];

        b = b.getRequiredBundle(b, 1, 2, 1, inputPath, 1, startTime, endTime);

        for (int i = 0; i < b.getClusters().size(); i++)
            Util.print(b.getDataSets().get(i));

        for (int i = 0; i < b.getDataSets().size(); i++)
            Util.print(b.getDataSets().get(i));

        Util.print(b.getProcessData());

        InstanceUtil.createDataWithinDatesAndPrefix(cluster,
                InstanceUtil.oozieDateToDate(InstanceUtil.addMinsToTime(startTime, -25)),
                InstanceUtil.oozieDateToDate(InstanceUtil.addMinsToTime(endTime, 25)),
                inputPath + "/input1/",
                1);
        InstanceUtil.createEmptyDirWithinDatesAndPrefix(cluster,
                InstanceUtil.oozieDateToDate(InstanceUtil.addMinsToTime(startTime, -25)),
                InstanceUtil.oozieDateToDate(InstanceUtil.addMinsToTime(endTime, 25)),
                inputPath + "/input0/",
                1);

        b.submitAndScheduleBundle(prism);

        Thread.sleep(20000);
        InstanceUtil
                .waitTillInstanceReachState(oozieClient, Util.getProcessName(b.getProcessData()),
                        2, CoordinatorAction.Status.SUCCEEDED, 10, ENTITY_TYPE.PROCESS);
    }

    @Test(enabled = true, groups = {"singleCluster"})
    public void optionalTest_allInputOptional() throws Exception {
        //process with 2 input , scheduled on single cluster
        // in input set true / false for both the input
        //create data after process has been scheduled, so that initially instance goes into waiting
        b = b.getRequiredBundle(b, 1, 2, 2, inputPath, 1, "2010-01-02T01:00Z",
                "2010-01-02T01:12Z");

        b.setProcessData(b.setProcessInputNames(b.getProcessData(), "inputData"));


        for (int i = 0; i < b.getClusters().size(); i++)
            Util.print(b.getDataSets().get(i));

        for (int i = 0; i < b.getDataSets().size(); i++)
            Util.print(b.getDataSets().get(i));

        Util.print(b.getProcessData());

        b.submitAndScheduleBundle(b, prism, false);

        Thread.sleep(20000);

        //instanceUtil.createDataWithinDatesAndPrefix(server1, instanceUtil.oozieDateToDate
        // ("2010-01-01T22:00Z")
        // , instanceUtil.oozieDateToDate("2010-01-02T04:00Z"), "/samarthData/input/input1/",
        // 1);
        InstanceUtil
                .waitTillInstanceReachState(oozieClient, Util.getProcessName(b.getProcessData()),
                        2, CoordinatorAction.Status.KILLED, 20, ENTITY_TYPE.PROCESS);
    }


    @Test(enabled = true, groups = {"singleCluster"})
    public void optionalTest_updateProcessMakeOptionalCompulsury() throws Exception {
        //initially 2 input and both are compulsury
        //process with 2 input , scheduled on single cluster
        // in input set true / false for both the input
        //create data after process has been scheduled, so that initially instance goes into waiting
        String startTime = InstanceUtil.getTimeWrtSystemTime(-4);
        String endTime = InstanceUtil.getTimeWrtSystemTime(30);

        b = b.getRequiredBundle(b, 1, 2, 1, inputPath, 1, startTime, endTime);

        for (int i = 0; i < b.getClusters().size(); i++)
            Util.print(b.getDataSets().get(i));

        for (int i = 0; i < b.getDataSets().size(); i++)
            Util.print(b.getDataSets().get(i));

        Util.print(b.getProcessData());

        b.submitAndScheduleBundle(b, prism, true);

        Thread.sleep(20000);
        InstanceUtil
                .waitTillInstanceReachState(oozieClient, Util.getProcessName(b.getProcessData()),
                        2, CoordinatorAction.Status.WAITING, 3, ENTITY_TYPE.PROCESS);

        InstanceUtil.createDataWithinDatesAndPrefix(cluster,
                InstanceUtil.oozieDateToDate(InstanceUtil.addMinsToTime(startTime, -25)),
                InstanceUtil.oozieDateToDate(InstanceUtil.addMinsToTime(endTime, 25)),
                inputPath + "/input1/",
                1);

        InstanceUtil
                .waitTillInstanceReachState(oozieClient, Util.getProcessName(b.getProcessData()),
                        1, CoordinatorAction.Status.SUCCEEDED, 20, ENTITY_TYPE.PROCESS);

        b.setProcessData(b.setProcessFeeds(b.getProcessData(), b.getDataSets(), 2, 0, 1));

        Util.print("modified process:" + b.getProcessData());

        prism.getProcessHelper().update(b.getProcessData(), b.getProcessData());

        Util.print("modified process:" + b.getProcessData());
        //from now on ... it should wait of input0 also

        Thread.sleep(60000);

        InstanceUtil
                .waitTillInstanceReachState(oozieClient, Util.getProcessName(b.getProcessData()),
                        2, CoordinatorAction.Status.WAITING, 3, ENTITY_TYPE.PROCESS);

        InstanceUtil.createDataWithinDatesAndPrefix(cluster,
                InstanceUtil.oozieDateToDate(InstanceUtil.addMinsToTime(startTime, -25)),
                InstanceUtil.oozieDateToDate(InstanceUtil.addMinsToTime(endTime, 25)),
                inputPath + "/input0/",
                1);

        InstanceUtil
                .waitTillInstanceReachState(oozieClient, Util.getProcessName(b.getProcessData()),
                        2, CoordinatorAction.Status.SUCCEEDED, 20, ENTITY_TYPE.PROCESS);
    }


    @Test(enabled = true, groups = {"singleCluster"})
    public void optionalTest_updateProcessMakeCompulsuryOptional() throws Exception {

        //initially 2 input and both are compulsury
        //process with 2 input , scheduled on single cluster
        // in input set true / false for both the input
        //create data after process has been scheduled, so that initially instance goes into waiting
        String startTime = InstanceUtil.getTimeWrtSystemTime(-4);
        String endTime = InstanceUtil.getTimeWrtSystemTime(30);

        b = b.getRequiredBundle(b, 1, 2, 1, inputPath, 1, startTime, endTime);

        for (int i = 0; i < b.getClusters().size(); i++)
            Util.print(b.getDataSets().get(i));

        for (int i = 0; i < b.getDataSets().size(); i++)
            Util.print(b.getDataSets().get(i));

        Util.print(b.getProcessData());

        b.submitAndScheduleBundle(b, prism, true);

        Thread.sleep(20000);
        InstanceUtil
                .waitTillInstanceReachState(oozieClient, Util.getProcessName(b.getProcessData()),
                        2, CoordinatorAction.Status.WAITING, 3, ENTITY_TYPE.PROCESS);

        InstanceUtil.createDataWithinDatesAndPrefix(cluster,
                InstanceUtil.oozieDateToDate(InstanceUtil.addMinsToTime(startTime, -25)),
                InstanceUtil.oozieDateToDate(InstanceUtil.addMinsToTime(endTime, 25)),
                inputPath + "/input1/",
                1);
        InstanceUtil
                .waitTillInstanceReachState(oozieClient, Util.getProcessName(b.getProcessData()),
                        1, CoordinatorAction.Status.SUCCEEDED, 20, ENTITY_TYPE.PROCESS);

        b.setProcessData(b.setProcessFeeds(b.getProcessData(), b.getDataSets(), 2, 2, 1));

        //delete all input data
        HadoopUtil.deleteDirIfExists(inputPath + "/", clusterFS);

        b.setProcessData(b.setProcessInputNames(b.getProcessData(), "inputData0", "inputData"));

        Util.print("modified process:" + b.getProcessData());


        prism.getProcessHelper().update(b.getProcessData(), b.getProcessData());

        Util.print("modified process:" + b.getProcessData());
        //from now on ... it should wait of input0 also

        Thread.sleep(30000);

        InstanceUtil
                .waitTillInstanceReachState(oozieClient, Util.getProcessName(b.getProcessData()),
                        2, CoordinatorAction.Status.KILLED, 10, ENTITY_TYPE.PROCESS);
    }
}
