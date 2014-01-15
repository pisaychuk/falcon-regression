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
import org.apache.falcon.regression.core.generated.feed.ActionType;
import org.apache.falcon.regression.core.generated.feed.ClusterType;
import org.apache.falcon.regression.core.interfaces.IEntityManagerHelper;
import org.apache.falcon.regression.core.response.ServiceResponse;
import org.apache.falcon.regression.core.supportClasses.ENTITY_TYPE;
import org.apache.falcon.regression.core.util.AssertUtil;
import org.apache.falcon.regression.core.util.InstanceUtil;
import org.apache.falcon.regression.core.util.Util;
import org.apache.falcon.regression.core.util.Util.URLS;
import org.apache.falcon.regression.core.util.XmlUtil;
import org.apache.falcon.regression.testHelper.BaseMultiClusterTests;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PrismFeedDeleteTest extends BaseMultiClusterTests {
    
    private Bundle bundle1;
    private Bundle bundle2;
    private boolean restartRequired;


    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method) throws Exception {
        Util.print("test name: " + method.getName());
        restartRequired = false;
        Bundle bundle = Util.readELBundles()[0][0];
        bundle1 = new Bundle(bundle, server1.getEnvFileName());
        bundle1.generateUniqueBundle();

        bundle2 = new Bundle(bundle, server2.getEnvFileName());
        bundle2.generateUniqueBundle();
    }
    
    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
        if (restartRequired) {
            Util.restartService(server1.getFeedHelper());
        }
    }
    
/** NOTE: All test cases assume that there are two entities scheduled in each colo */

    @Test(groups = {"multiCluster"})
    public void testServer1FeedDeleteInBothColos() throws Exception {
        for (String cluster : bundle1.getClusters()) {
            Util.assertSucceeded(prism.getClusterHelper().submitEntity(Util.URLS.SUBMIT_URL, cluster));
        }

        Util.assertSucceeded(prism.getFeedHelper().submitEntity(Util.URLS.SUBMIT_URL, bundle1.getDataSets().get(0)));
        //fetch the initial store and archive state for prism
        List<String> initialPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> initialPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the initial store and archive for both colos
        List<String> initialServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> initialServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> initialServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> initialServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();

        //lets now delete the cluster from both colos
        Util.assertSucceeded(prism.getFeedHelper().delete(Util.URLS.DELETE_URL, bundle1.getDataSets().get(0)));

        //now lets get the final states
        List<String> finalPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> finalPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the final store and archive for both colos
        List<String> finalServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> finalServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> finalServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> finalServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();

        //now ensure that data has been deleted from all cluster store and is present in the cluster archives

        String feedName = Util.readDatasetName(bundle1.getDataSets().get(0));
        //prism:
        compareDataStoreStates(initialPrismStore, finalPrismStore, feedName);
        compareDataStoreStates(finalPrismArchiveStore, initialPrismArchiveStore, feedName);

        //server1:
        compareDataStoreStates(initialServer1Store, finalServer1Store, feedName);
        compareDataStoreStates(finalServer1ArchiveStore, initialServer1ArchiveStore, feedName);

        //server2:
        compareDataStoresForEquality(initialServer2Store, finalServer2Store);
        compareDataStoresForEquality(finalServer2ArchiveStore, initialServer2ArchiveStore);

    }

    @Test(groups = {"multiCluster"})
    public void testServer1FeedDeleteWhen1ColoIsDown() throws Exception {
        restartRequired = true;

        for (String cluster : bundle1.getClusters()) {
            Util.assertSucceeded(prism.getClusterHelper().submitEntity(Util.URLS.SUBMIT_URL, cluster));
        }

        Util.assertSucceeded(prism.getFeedHelper().submitEntity(Util.URLS.SUBMIT_URL, bundle1.getDataSets().get(0)));
        //fetch the initial store and archive state for prism
        List<String> initialPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> initialPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the initial store and archive for both colos
        List<String> initialServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> initialServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> initialServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> initialServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();


        //bring down Server2 colo :P
        Util.shutDownService(server1.getFeedHelper());

        //lets now delete the cluster from both colos
        Util.assertFailed(prism.getFeedHelper().delete(Util.URLS.DELETE_URL, bundle1.getDataSets().get(0)));

        //now lets get the final states
        List<String> finalPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> finalPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the final store and archive for both colos
        List<String> finalServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> finalServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> finalServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> finalServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();

        //now ensure that data has been deleted from all cluster store and is present in the cluster archives

        String clusterName = Util.readDatasetName(bundle1.getDataSets().get(0));
        //prism:
        compareDataStoresForEquality(initialPrismStore, finalPrismStore);
        compareDataStoresForEquality(finalPrismArchiveStore, initialPrismArchiveStore);

        //Server2:
        compareDataStoresForEquality(initialServer2Store, finalServer2Store);
        compareDataStoresForEquality(finalServer2ArchiveStore, initialServer2ArchiveStore);

        //Server1:
        compareDataStoresForEquality(initialServer1Store, finalServer1Store);
        compareDataStoresForEquality(initialServer1ArchiveStore, finalServer1ArchiveStore);

        Util.startService(server1.getFeedHelper());

        Util.assertSucceeded(prism.getFeedHelper().delete(Util.URLS.DELETE_URL, bundle1.getDataSets().get(0)));

        List<String> Server2ArchivePostUp = server2.getFeedHelper().getArchiveInfo();
        List<String> Server2StorePostUp = server2.getFeedHelper().getStoreInfo();

        List<String> Server1ArchivePostUp = server1.getFeedHelper().getArchiveInfo();
        List<String> Server1StorePostUp = server1.getFeedHelper().getStoreInfo();

        List<String> prismHelperArchivePostUp = prism.getFeedHelper().getArchiveInfo();
        List<String> prismHelperStorePostUp = prism.getFeedHelper().getStoreInfo();

        compareDataStoreStates(finalPrismStore, prismHelperStorePostUp, clusterName);
        compareDataStoreStates(prismHelperArchivePostUp, finalPrismArchiveStore, clusterName);

        compareDataStoreStates(initialServer1Store, Server1StorePostUp, clusterName);
        compareDataStoreStates(Server1ArchivePostUp, finalServer1ArchiveStore, clusterName);

        compareDataStoresForEquality(finalServer2Store, Server2StorePostUp);
        compareDataStoresForEquality(finalServer2ArchiveStore, Server2ArchivePostUp);
    }
  

    @Test(groups = {"multiCluster"})
    public void testServer1FeedDeleteAlreadyDeletedFeed() throws Exception {
        restartRequired = true;
        for (String cluster : bundle1.getClusters()) {
            Util.assertSucceeded(prism.getClusterHelper().submitEntity(Util.URLS.SUBMIT_URL, cluster));
        }

        Util.assertSucceeded(prism.getFeedHelper().submitEntity(Util.URLS.SUBMIT_URL, bundle1.getDataSets().get(0)));
        Util.assertSucceeded(prism.getFeedHelper().delete(Util.URLS.DELETE_URL, bundle1.getDataSets().get(0)));

        //fetch the initial store and archive state for prism
        List<String> initialPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> initialPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the initial store and archive for both colos
        List<String> initialServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> initialServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> initialServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> initialServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();

        Util.assertSucceeded(prism.getFeedHelper().delete(Util.URLS.DELETE_URL, bundle1.getDataSets().get(0)));

        //now lets get the final states
        List<String> finalPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> finalPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the final store and archive for both colos
        List<String> finalServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> finalServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> finalServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> finalServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();

        //now ensure that data has been deleted from all cluster store and is present in the cluster archives

        //prism:
        compareDataStoresForEquality(initialPrismStore, finalPrismStore);
        compareDataStoresForEquality(initialPrismArchiveStore, finalPrismArchiveStore);

        //Server2:
        compareDataStoresForEquality(initialServer2Store, finalServer2Store);
        compareDataStoresForEquality(initialServer2ArchiveStore, finalServer2ArchiveStore);

        //Server1:
        compareDataStoresForEquality(initialServer1Store, finalServer1Store);
        compareDataStoresForEquality(initialServer1ArchiveStore, finalServer1ArchiveStore);
    }


    @Test(groups = {"multiCluster"})
    public void testServer1FeedDeleteTwiceWhen1ColoIsDownDuring1stDelete() throws Exception {
        restartRequired = true;

        for (String cluster : bundle1.getClusters()) {
            Util.assertSucceeded(prism.getClusterHelper().submitEntity(Util.URLS.SUBMIT_URL, cluster));
        }

        Util.assertSucceeded(prism.getFeedHelper().submitEntity(Util.URLS.SUBMIT_URL, bundle1.getDataSets().get(0)));

        Util.shutDownService(server1.getClusterHelper());


        //lets now delete the cluster from both colos
        Util.assertFailed(prism.getFeedHelper().delete(Util.URLS.DELETE_URL, bundle1.getDataSets().get(0)));

        //now lets get the final states
        List<String> initialPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> initialPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the initial store and archive for both colos
        List<String> initialServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> initialServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> initialServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> initialServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();

        //start up service
        Util.startService(server1.getFeedHelper());

        //delete again
        Util.assertSucceeded(prism.getFeedHelper().delete(Util.URLS.DELETE_URL, bundle1.getDataSets().get(0)));

        //get final states
        List<String> finalPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> finalPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the final store and archive for both colos
        List<String> finalServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> finalServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> finalServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> finalServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();

        //now ensure that data has been deleted from all cluster store and is present in the cluster archives

        String clusterName = Util.readDatasetName(bundle1.getDataSets().get(0));

        //prism:
        compareDataStoreStates(initialPrismStore, finalPrismStore, clusterName);
        compareDataStoreStates(finalPrismArchiveStore, initialPrismArchiveStore, clusterName);

        //Server2:
        compareDataStoresForEquality(initialServer2Store, finalServer2Store);
        compareDataStoresForEquality(initialServer2ArchiveStore, finalServer2ArchiveStore);

        //Server1:
        compareDataStoreStates(initialServer1Store, finalServer1Store, clusterName);
        compareDataStoreStates(finalServer1ArchiveStore, initialServer1ArchiveStore, clusterName);
    }

    @Test(groups = {"multiCluster"})
    public void testServer1FeedDeleteNonExistent() throws Exception {
        //now lets get the final states
        List<String> initialPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> initialPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the initial store and archive for both colos
        List<String> initialServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> initialServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> initialServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> initialServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();

        Util.assertSucceeded(prism.getFeedHelper().delete(Util.URLS.DELETE_URL, bundle1.getDataSets().get(0)));

        //get final states
        List<String> finalPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> finalPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the final store and archive for both colos
        List<String> finalServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> finalServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> finalServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> finalServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();

        //now ensure that data has been deleted from all cluster store and is present in the  cluster archives

        //prism:
        compareDataStoresForEquality(initialPrismStore, finalPrismStore);
        compareDataStoresForEquality(initialPrismArchiveStore, finalPrismArchiveStore);

        //Server2:
        compareDataStoresForEquality(initialServer2Store, finalServer2Store);
        compareDataStoresForEquality(initialServer2ArchiveStore, finalServer2ArchiveStore);

        //Server1:
        compareDataStoresForEquality(initialServer1Store, finalServer1Store);
        compareDataStoresForEquality(initialServer1ArchiveStore, finalServer1ArchiveStore);
    }


    @Test(groups = {"multiCluster"})
    public void testServer1FeedDeleteNonExistentWhen1ColoIsDownDuringDelete() throws Exception {
        restartRequired = true;
        bundle1 = new Bundle(bundle1, server1.getEnvFileName());
        bundle2 = new Bundle(bundle2, server2.getEnvFileName());

        this.bundle1.setCLusterColo(server1.getClusterHelper().getColo().split("=")[1]);
        Util.print("cluster bundle1: " + bundle1.getClusters().get(0));

        ServiceResponse r = prism.getClusterHelper()
                .submitEntity(URLS.SUBMIT_URL, bundle1.getClusters().get(0));
        Assert.assertTrue(r.getMessage().contains("SUCCEEDED"));

        this.bundle2.setCLusterColo(server2.getClusterHelper().getColo().split("=")[1]);
        Util.print("cluster bundle2: " + bundle2.getClusters().get(0));
        r = prism.getClusterHelper().submitEntity(URLS.SUBMIT_URL, bundle2.getClusters().get(0));
        Assert.assertTrue(r.getMessage().contains("SUCCEEDED"));

        String startTimeServer1 = "2012-10-01T12:00Z";
        String startTimeServer2 = "2012-10-01T12:00Z";

        String feed = bundle1.getDataSets().get(0);
        feed = InstanceUtil.setFeedCluster(feed,
                XmlUtil.createValidity("2012-10-01T12:00Z", "2010-01-01T00:00Z"),
                XmlUtil.createRtention("days(10000)", ActionType.DELETE), null,
                ClusterType.SOURCE, null, null);
        
        feed = InstanceUtil.setFeedCluster(feed, XmlUtil.createValidity(startTimeServer1, "2099-10-01T12:10Z"),
                        XmlUtil.createRtention("days(10000)", ActionType.DELETE),
                        Util.readClusterName(bundle1.getClusters().get(0)), ClusterType.SOURCE,
                        "${cluster.colo}",
                        baseHDFSDir + "/localDC/rc/billing/${YEAR}/${MONTH}/${DAY}/${HOUR}/${MINUTE}");
        
        feed = InstanceUtil.setFeedCluster(feed, XmlUtil.createValidity(startTimeServer2, "2099-10-01T12:25Z"),
                        XmlUtil.createRtention("days(10000)", ActionType.DELETE),
                        Util.readClusterName(bundle2.getClusters().get(0)), ClusterType.TARGET, null,
                        baseHDFSDir + "/clusterPath/localDC/rc/billing/${YEAR}/${MONTH}/${DAY}/${HOUR}/${MINUTE}");

        Util.shutDownService(server1.getFeedHelper());

        ServiceResponse response = prism.getFeedHelper().delete(Util.URLS.DELETE_URL, feed);
        Util.assertSucceeded(response);
    }


    @Test(groups = {"multiCluster"})
    public void testDeleteFeedScheduledInOneColo() throws Exception {
        submitAndScheduleFeed(bundle1);
        submitAndScheduleFeed(bundle2);

        //fetch the initial store and archive state for prism
        List<String> initialPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> initialPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the initial store and archive for both colos
        List<String> initialServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> initialServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> initialServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> initialServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();

        //lets now delete the cluster from both colos
        Util.assertSucceeded(prism.getFeedHelper().delete(Util.URLS.DELETE_URL, bundle1.getDataSets().get(0)));

        //now lets get the final states
        List<String> finalPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> finalPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the final store and archive for both colos
        List<String> finalServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> finalServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> finalServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> finalServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();

        //now ensure that data has been deleted from all cluster store and is present in the
        // cluster archives

        String clusterName = Util.readDatasetName(bundle1.getDataSets().get(0));
        //prism:
        compareDataStoreStates(initialPrismStore, finalPrismStore, clusterName);
        compareDataStoreStates(finalPrismArchiveStore, initialPrismArchiveStore, clusterName);

        //Server1:
        compareDataStoreStates(initialServer1Store, finalServer1Store, clusterName);
        compareDataStoreStates(finalServer1ArchiveStore, initialServer1ArchiveStore, clusterName);

        //Server2:
        compareDataStoresForEquality(initialServer2Store, finalServer2Store);
        compareDataStoresForEquality(finalServer2ArchiveStore, initialServer2ArchiveStore);


    }

    @Test(groups = {"multiCluster"})
    public void testDeleteFeedSuspendedInOneColo() throws Exception {
        submitAndScheduleFeed(bundle1);
        submitAndScheduleFeed(bundle2);

        //suspend Server1 colo thingy
        Util.assertSucceeded(prism.getFeedHelper().suspend(URLS.SUSPEND_URL, bundle1.getDataSets().get(0)));

        //fetch the initial store and archive state for prism
        List<String> initialPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> initialPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the initial store and archive for both colos
        List<String> initialServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> initialServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> initialServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> initialServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();

        //lets now delete the cluster from both colos
        Util.assertSucceeded(prism.getFeedHelper().delete(Util.URLS.DELETE_URL, bundle1.getDataSets().get(0)));

        //now lets get the final states
        List<String> finalPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> finalPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the final store and archive for both colos
        List<String> finalServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> finalServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> finalServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> finalServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();

        //now ensure that data has been deleted from all cluster store and is present in the cluster archives

        String clusterName = Util.readDatasetName(bundle1.getDataSets().get(0));
        //prism:
        compareDataStoreStates(initialPrismStore, finalPrismStore, clusterName);
        compareDataStoreStates(finalPrismArchiveStore, initialPrismArchiveStore, clusterName);

        //Server1:
        compareDataStoreStates(initialServer1Store, finalServer1Store, clusterName);
        compareDataStoreStates(finalServer1ArchiveStore, initialServer1ArchiveStore, clusterName);

        //Server2:
        compareDataStoresForEquality(initialServer2Store, finalServer2Store);
        compareDataStoresForEquality(finalServer2ArchiveStore, initialServer2ArchiveStore);


    }


    @Test(groups = {"multiCluster"})
    public void testDeleteFeedSuspendedInOneColoWhileBothFeedsAreSuspended() throws Exception {
        submitAndScheduleFeed(bundle1);
        submitAndScheduleFeed(bundle2);

        //suspend Server1 colo thingy
        Util.assertSucceeded(prism.getFeedHelper().suspend(URLS.SUSPEND_URL, bundle1.getDataSets().get(0)));
        Util.assertSucceeded(prism.getFeedHelper().suspend(URLS.SUSPEND_URL, bundle2.getDataSets().get(0)));

        //fetch the initial store and archive state for prism
        List<String> initialPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> initialPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the initial store and archive for both colos
        List<String> initialServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> initialServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> initialServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> initialServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();

        //lets now delete the cluster from both colos
        Util.assertSucceeded(prism.getFeedHelper().delete(Util.URLS.DELETE_URL, bundle1.getDataSets().get(0)));

        //now lets get the final states
        List<String> finalPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> finalPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the final store and archive for both colos
        List<String> finalServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> finalServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> finalServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> finalServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();

        //now ensure that data has been deleted from all cluster store and is present in the cluster archives

        String clusterName = Util.readDatasetName(bundle1.getDataSets().get(0));
        //prism:
        compareDataStoreStates(initialPrismStore, finalPrismStore, clusterName);
        compareDataStoreStates(finalPrismArchiveStore, initialPrismArchiveStore, clusterName);

        //Server1:
        compareDataStoreStates(initialServer1Store, finalServer1Store, clusterName);
        compareDataStoreStates(finalServer1ArchiveStore, initialServer1ArchiveStore, clusterName);

        //Server2:
        compareDataStoresForEquality(initialServer2Store, finalServer2Store);
        compareDataStoresForEquality(finalServer2ArchiveStore, initialServer2ArchiveStore);
    }

    @Test(groups = {"multiCluster"})
    public void testDeleteFeedSuspendedInOneColoWhileThatColoIsDown()
    throws Exception {
        restartRequired = true;

        submitAndScheduleFeed(bundle1);
        submitAndScheduleFeed(bundle2);

        Util.assertSucceeded(prism.getFeedHelper().suspend(Util.URLS.SUSPEND_URL, bundle1.getDataSets().get(0)));

        //fetch the initial store and archive state for prism
        List<String> initialPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> initialPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the initial store and archive for both colos
        List<String> initialServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> initialServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> initialServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> initialServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();

        //shutdown Server1
        Util.shutDownService(server1.getFeedHelper());

        //lets now delete the cluster from both colos
        Util.assertFailed(prism.getFeedHelper().delete(Util.URLS.DELETE_URL, bundle1.getDataSets().get(0)));

        //now lets get the final states
        List<String> finalPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> finalPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the final store and archive for both colos
        List<String> finalServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> finalServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> finalServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> finalServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();

        //now ensure that data has been deleted from all cluster store and is present in the cluster archives

        String clusterName = Util.readDatasetName(bundle1.getDataSets().get(0));
        //prism:
        compareDataStoresForEquality(initialPrismStore, finalPrismStore);
        compareDataStoresForEquality(finalPrismArchiveStore, initialPrismArchiveStore);

        //Server1:
        compareDataStoresForEquality(initialServer1Store, finalServer1Store);
        compareDataStoresForEquality(initialServer1ArchiveStore, finalServer1ArchiveStore);

        //Server2:
        compareDataStoresForEquality(initialServer2Store, finalServer2Store);
        compareDataStoresForEquality(finalServer2ArchiveStore, initialServer2ArchiveStore);

        Util.startService(server1.getClusterHelper());

        Util.assertSucceeded(prism.getFeedHelper().delete(Util.URLS.DELETE_URL, bundle1.getDataSets().get(0)));

        List<String> Server1StorePostUp = server1.getFeedHelper().getStoreInfo();
        List<String> Server1ArchivePostUp = server1.getFeedHelper().getArchiveInfo();

        List<String> Server2StorePostUp = server2.getFeedHelper().getStoreInfo();
        List<String> Server2ArchivePostUp = server2.getFeedHelper().getArchiveInfo();

        List<String> prismStorePostUp = prism.getFeedHelper().getStoreInfo();
        List<String> prismArchivePostUp = prism.getFeedHelper().getArchiveInfo();


        compareDataStoresForEquality(Server2StorePostUp, finalServer2Store);
        compareDataStoresForEquality(Server2ArchivePostUp, finalServer2ArchiveStore);

        compareDataStoreStates(finalServer1Store, Server1StorePostUp, clusterName);
        compareDataStoreStates(Server1ArchivePostUp, finalServer1ArchiveStore, clusterName);

        compareDataStoreStates(finalPrismStore, prismStorePostUp, clusterName);
        compareDataStoreStates(prismArchivePostUp, finalPrismArchiveStore, clusterName);
    }


    @Test(groups = {"multiCluster"})
    public void testDeleteFeedSuspendedInOneColoWhileThatColoIsDownAndOtherHasSuspendedFeed() throws Exception {
        restartRequired = true;

        submitAndScheduleFeed(bundle1);
        submitAndScheduleFeed(bundle2);

        Util.assertSucceeded(prism.getFeedHelper().suspend(Util.URLS.SUSPEND_URL, bundle1.getDataSets().get(0)));
        Util.assertSucceeded(prism.getFeedHelper().suspend(Util.URLS.SUSPEND_URL, bundle2.getDataSets().get(0)));
        //fetch the initial store and archive state for prism
        List<String> initialPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> initialPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the initial store and archive for both colos
        List<String> initialServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> initialServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> initialServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> initialServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();

        //shutdown Server1
        Util.shutDownService(server1.getFeedHelper());

        //lets now delete the feed from both colos
        Util.assertFailed(prism.getFeedHelper().delete(Util.URLS.DELETE_URL, bundle1.getDataSets().get(0)));

        //now lets get the final states
        List<String> finalPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> finalPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the final store and archive for both colos
        List<String> finalServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> finalServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> finalServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> finalServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();

        //now ensure that data has been deleted from all cluster store and is present in the
        // cluster archives

        String clusterName = Util.readDatasetName(bundle1.getDataSets().get(0));
        //prism:
        compareDataStoresForEquality(initialPrismStore, finalPrismStore);
        compareDataStoresForEquality(finalPrismArchiveStore, initialPrismArchiveStore);

        //Server1:
        compareDataStoresForEquality(initialServer1Store, finalServer1Store);
        compareDataStoresForEquality(initialServer1ArchiveStore, finalServer1ArchiveStore);

        //Server2:
        compareDataStoresForEquality(initialServer2Store, finalServer2Store);
        compareDataStoresForEquality(finalServer2ArchiveStore, initialServer2ArchiveStore);

        Util.startService(server1.getFeedHelper());

        Util.assertSucceeded(prism.getFeedHelper().delete(Util.URLS.DELETE_URL, bundle1.getDataSets().get(0)));

        HashMap<String, List<String>> finalSystemState = getSystemState(ENTITY_TYPE.DATA);

        compareDataStoreStates(finalSystemState.get("prismArchive"), finalPrismArchiveStore, clusterName);
        compareDataStoreStates(finalPrismStore, finalSystemState.get("prismStore"), clusterName);

        compareDataStoreStates(finalServer1Store, finalSystemState.get("Server1Store"), clusterName);
        compareDataStoreStates(finalSystemState.get("Server1Archive"), finalServer1ArchiveStore, clusterName);

        compareDataStoresForEquality(finalSystemState.get("Server2Archive"), finalServer2ArchiveStore);
        compareDataStoresForEquality(finalSystemState.get("Server2Store"), finalServer2Store);
    }

    @Test(groups = {"multiCluster"})
    public void testDeleteFeedScheduledInOneColoWhileThatColoIsDown() throws Exception {
        restartRequired = true;

        submitAndScheduleFeed(bundle1);
        submitAndScheduleFeed(bundle2);

        //fetch the initial store and archive state for prism
        List<String> initialPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> initialPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the initial store and archive for both colos
        List<String> initialServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> initialServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> initialServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> initialServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();

        //shutdown Server1
        Util.shutDownService(server1.getFeedHelper());

        //lets now delete the cluster from both colos
        Util.assertFailed(prism.getFeedHelper().delete(Util.URLS.DELETE_URL, bundle1.getDataSets().get(0)));

        //now lets get the final states
        List<String> finalPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> finalPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the final store and archive for both colos
        List<String> finalServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> finalServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> finalServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> finalServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();

        //now ensure that data has been deleted from all cluster store and is present in the
        // cluster archives

        String clusterName = Util.readDatasetName(bundle1.getDataSets().get(0));
        //prism:
        compareDataStoresForEquality(initialPrismStore, finalPrismStore);
        compareDataStoresForEquality(finalPrismArchiveStore, initialPrismArchiveStore);

        //Server1:
        compareDataStoresForEquality(initialServer1Store, finalServer1Store);
        compareDataStoresForEquality(initialServer1ArchiveStore, finalServer1ArchiveStore);

        //Server2:
        compareDataStoresForEquality(initialServer2Store, finalServer2Store);
        compareDataStoresForEquality(finalServer2ArchiveStore, initialServer2ArchiveStore);


        Util.startService(server1.getClusterHelper());
        Util.assertSucceeded(prism.getFeedHelper().delete(Util.URLS.DELETE_URL, bundle1.getDataSets().get(0)));

        HashMap<String, List<String>> systemStatePostUp = getSystemState(ENTITY_TYPE.DATA);

        compareDataStoreStates(finalPrismStore, systemStatePostUp.get("prismStore"), clusterName);
        compareDataStoreStates(systemStatePostUp.get("prismArchive"), finalPrismArchiveStore, clusterName);

        compareDataStoreStates(finalServer1Store, systemStatePostUp.get("Server1Store"), clusterName);
        compareDataStoreStates(systemStatePostUp.get("Server1Archive"), finalServer1ArchiveStore, clusterName);

        compareDataStoresForEquality(finalServer2ArchiveStore, systemStatePostUp.get("Server2Archive"));
        compareDataStoresForEquality(finalServer2Store, systemStatePostUp.get("Server2Store"));
    }

    @Test(groups = {"multiCluster"})
    public void testDeleteFeedSuspendedInOneColoWhileAnotherColoIsDown() throws Exception {
        restartRequired = true;

        bundle1.setCLusterColo(server1.getClusterHelper().getColo().split("=")[1]);
        Util.print("cluster bundle1: " + bundle1.getClusters().get(0));

        ServiceResponse r = prism.getClusterHelper().submitEntity(URLS.SUBMIT_URL, bundle1.getClusters().get(0));
        Assert.assertTrue(r.getMessage().contains("SUCCEEDED"));

        bundle2.setCLusterColo(server2.getClusterHelper().getColo().split("=")[1]);
        Util.print("cluster bundle2: " + bundle2.getClusters().get(0));
        r = prism.getClusterHelper().submitEntity(URLS.SUBMIT_URL, bundle2.getClusters().get(0));
        Assert.assertTrue(r.getMessage().contains("SUCCEEDED"));

        String startTimeServer1 = "2012-10-01T12:00Z";
        String startTimeServer2 = "2012-10-01T12:00Z";

        String feed = bundle1.getDataSets().get(0);
        feed = InstanceUtil.setFeedCluster(feed,
                XmlUtil.createValidity("2012-10-01T12:00Z", "2010-01-01T00:00Z"),
                XmlUtil.createRtention("days(10000)", ActionType.DELETE), null,
                ClusterType.SOURCE, null, null);

        feed = InstanceUtil.setFeedCluster(feed, XmlUtil.createValidity(startTimeServer1, "2099-10-01T12:10Z"),
                        XmlUtil.createRtention("days(10000)", ActionType.DELETE),
                        Util.readClusterName(bundle1.getClusters().get(0)), ClusterType.SOURCE,
                        "${cluster.colo}",
                        baseHDFSDir + "/localDC/rc/billing/${YEAR}/${MONTH}/${DAY}/${HOUR}/${MINUTE}");

        feed = InstanceUtil.setFeedCluster(feed, XmlUtil.createValidity(startTimeServer2, "2099-10-01T12:25Z"),
                        XmlUtil.createRtention("days(10000)", ActionType.DELETE),
                        Util.readClusterName(bundle2.getClusters().get(0)), ClusterType.TARGET, null,
                        baseHDFSDir + "/clusterPath/localDC/rc/billing/${YEAR}/${MONTH}/${DAY}/${HOUR}/${MINUTE}");

        Util.print("feed: " + feed);

        r = prism.getFeedHelper().submitEntity(URLS.SUBMIT_URL, feed);

        AssertUtil.assertSucceeded(r);

        r = prism.getFeedHelper().schedule(URLS.SCHEDULE_URL, feed);
        AssertUtil.assertSucceeded(r);
        Thread.sleep(15000);

        //fetch the initial store and archive state for prism
        List<String> initialPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> initialPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the initial store and archive for both colos
        List<String> initialServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> initialServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> initialServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> initialServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();

        Util.shutDownService(server1.getFeedHelper());

        r = prism.getFeedHelper().suspend(URLS.SUSPEND_URL, feed);
        Thread.sleep(10000);
        Util.assertPartialSucceeded(r);                
        Assert.assertTrue(r.getMessage().contains("Connection refused"
                + server2.getClusterHelper().getColo().split("=")[1] + "/" + Util.getFeedName(feed)));

        ServiceResponse response = prism.getFeedHelper().delete(Util.URLS.DELETE_URL, feed);
        Assert.assertTrue(response.getMessage().contains("Connection refused"
                + server2.getClusterHelper().getColo().split("=")[1] + "/" + Util.getFeedName(feed)));
        Util.assertPartialSucceeded(response);

        //now lets get the final states
        List<String> finalPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> finalPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the final store and archive for both colos
        List<String> finalServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> finalServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> finalServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> finalServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();

        //now ensure that data has been deleted from all cluster store and is present in the cluster archives

        String clusterName = Util.readDatasetName(bundle1.getDataSets().get(0));
        //prism:
        compareDataStoresForEquality(initialPrismStore, finalPrismStore);
        compareDataStoresForEquality(finalPrismArchiveStore, initialPrismArchiveStore);

        //Server1:
        compareDataStoresForEquality(initialServer1Store, finalServer1Store);
        compareDataStoresForEquality(finalServer1ArchiveStore, initialServer1ArchiveStore);

        //Server2:
        compareDataStoreStates(initialServer2Store, finalServer2Store, clusterName);
        compareDataStoreStates(finalServer2ArchiveStore, initialServer2ArchiveStore, clusterName);
    }

    @Test(enabled = true)
    public void testDeleteFeedSuspendedInOneColoWhileAnotherColoIsDownWithFeedSuspended() throws Exception {
        restartRequired = true;

        this.bundle1.setCLusterColo(server1.getClusterHelper().getColo().split("=")[1]);
        Util.print("cluster bundle1: " + bundle1.getClusters().get(0));

        ServiceResponse r = prism.getClusterHelper().submitEntity(URLS.SUBMIT_URL, bundle1.getClusters().get(0));
        Assert.assertTrue(r.getMessage().contains("SUCCEEDED"));

        this.bundle2.setCLusterColo(server2.getClusterHelper().getColo().split("=")[1]);
        Util.print("cluster bundle2: " + bundle2.getClusters().get(0));
        r = prism.getClusterHelper().submitEntity(URLS.SUBMIT_URL, bundle2.getClusters().get(0));
        Assert.assertTrue(r.getMessage().contains("SUCCEEDED"));

        String startTimeServer1 = "2012-10-01T12:00Z";
        String startTimeServer2 = "2012-10-01T12:00Z";

        String feed = bundle1.getDataSets().get(0);
        feed = InstanceUtil.setFeedCluster(feed,
                XmlUtil.createValidity("2012-10-01T12:00Z", "2010-01-01T00:00Z"),
                XmlUtil.createRtention("days(10000)", ActionType.DELETE), null,
                ClusterType.SOURCE, null, null);
        feed = InstanceUtil
                .setFeedCluster(feed, XmlUtil.createValidity(startTimeServer1, "2099-10-01T12:10Z"),
                        XmlUtil.createRtention("days(10000)", ActionType.DELETE),
                        Util.readClusterName(bundle1.getClusters().get(0)), ClusterType.SOURCE,
                        "${cluster.colo}",
                        baseHDFSDir + "/localDC/rc/billing/${YEAR}/${MONTH}/${DAY}/${HOUR}/${MINUTE}");
        feed = InstanceUtil
                .setFeedCluster(feed, XmlUtil.createValidity(startTimeServer2, "2099-10-01T12:25Z"),
                        XmlUtil.createRtention("days(10000)", ActionType.DELETE),
                        Util.readClusterName(bundle2.getClusters().get(0)), ClusterType.TARGET, null,
                        baseHDFSDir + "/clusterPath/localDC/rc/billing/${YEAR}/${MONTH}/${DAY}/${HOUR}/$" +
                                "{MINUTE}");

        Util.print("feed: " + feed);

        r = prism.getFeedHelper().submitEntity(URLS.SUBMIT_URL, feed);

        AssertUtil.assertSucceeded(r);

        r = prism.getFeedHelper().schedule(URLS.SCHEDULE_URL, feed);
        AssertUtil.assertSucceeded(r);
        Thread.sleep(15000);

        //fetch the initial store and archive state for prism
        List<String> initialPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> initialPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the initial store and archive for both colos
        List<String> initialServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> initialServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> initialServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> initialServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();

        r = prism.getFeedHelper().suspend(URLS.SUSPEND_URL, feed);
        TimeUnit.SECONDS.sleep(10);
        AssertUtil.assertSucceeded(r);

        Util.shutDownService(server1.getFeedHelper());

        ServiceResponse response = prism.getFeedHelper().delete(Util.URLS.DELETE_URL, feed);
        Assert.assertTrue(response.getMessage().contains("Connection refused"
                + server2.getClusterHelper().getColo().split("=")[1] + "/" + Util.getFeedName(feed)));
        Util.assertPartialSucceeded(response);

        //now lets get the final states
        List<String> finalPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> finalPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the final store and archive for both colos
        List<String> finalServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> finalServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> finalServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> finalServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();

        //now ensure that data has been deleted from all cluster store and is present in the cluster archives

        String clusterName = Util.readDatasetName(bundle1.getDataSets().get(0));
        //prism:
        compareDataStoresForEquality(initialPrismStore, finalPrismStore);
        compareDataStoresForEquality(finalPrismArchiveStore, initialPrismArchiveStore);

        //Server1:
        compareDataStoresForEquality(initialServer1Store, finalServer1Store);
        compareDataStoresForEquality(finalServer1ArchiveStore, initialServer1ArchiveStore);

        //Server2:
        compareDataStoreStates(initialServer2Store, finalServer2Store, clusterName);
        compareDataStoreStates(finalServer2ArchiveStore, initialServer2ArchiveStore, clusterName);
    }


    @Test(groups = {"multiCluster"})
    public void testDeleteFeedScheduledInOneColoWhileAnotherColoIsDown() throws Exception {
        restartRequired = true;

        submitAndScheduleFeed(bundle1);
        submitAndScheduleFeed(bundle2);

        //fetch the initial store and archive state for prism
        List<String> initialPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> initialPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the initial store and archive for both colos
        List<String> initialServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> initialServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> initialServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> initialServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();

        //shutdown Server1
        Util.shutDownService(server1.getFeedHelper());

        //lets now delete the cluster from both colos
        Util.assertSucceeded(prism.getFeedHelper().delete(Util.URLS.DELETE_URL, bundle2.getDataSets().get(0)));

        //now lets get the final states
        List<String> finalPrismStore = prism.getFeedHelper().getStoreInfo();
        List<String> finalPrismArchiveStore = prism.getFeedHelper().getArchiveInfo();

        //fetch the final store and archive for both colos
        List<String> finalServer1Store = server1.getFeedHelper().getStoreInfo();
        List<String> finalServer1ArchiveStore = server1.getFeedHelper().getArchiveInfo();

        List<String> finalServer2Store = server2.getFeedHelper().getStoreInfo();
        List<String> finalServer2ArchiveStore = server2.getFeedHelper().getArchiveInfo();

        //now ensure that data has been deleted from all cluster store and is present in the
        // cluster archives

        String clusterName = Util.readDatasetName(bundle2.getDataSets().get(0));
        //prism:
        compareDataStoreStates(initialPrismStore, finalPrismStore, clusterName);
        compareDataStoreStates(finalPrismArchiveStore, initialPrismArchiveStore, clusterName);

        //Server1:
        compareDataStoresForEquality(initialServer1Store, finalServer1Store);
        compareDataStoresForEquality(initialServer1ArchiveStore, finalServer1ArchiveStore);

        //Server2:
        compareDataStoreStates(initialServer2Store, finalServer2Store, clusterName);
        compareDataStoreStates(finalServer2ArchiveStore, initialServer2ArchiveStore, clusterName);

        Util.startService(server1.getFeedHelper());

        Util.assertSucceeded(prism.getFeedHelper().delete(Util.URLS.DELETE_URL, bundle1.getDataSets().get(0)));

        clusterName = Util.readDatasetName(bundle1.getDataSets().get(0));

        HashMap<String, List<String>> systemPostUp = getSystemState(ENTITY_TYPE.DATA);

        compareDataStoreStates(systemPostUp.get("prismArchive"), finalPrismArchiveStore, clusterName);
        compareDataStoreStates(finalPrismStore, systemPostUp.get("prismStore"), clusterName);

        compareDataStoreStates(systemPostUp.get("Server1Archive"), finalServer1ArchiveStore, clusterName);
        compareDataStoreStates(finalServer1Store, systemPostUp.get("Server1Store"), clusterName);

        compareDataStoresForEquality(finalServer2ArchiveStore, systemPostUp.get("Server2Archive"));
        compareDataStoresForEquality(finalServer2Store, systemPostUp.get("Server2Store"));
    }

    private void compareDataStoreStates(List<String> initialState, List<String> finalState, String filename)
            throws Exception {
        List<String> temp = new ArrayList<String>();
        temp.addAll(initialState);
        temp.removeAll(finalState);
        Assert.assertEquals(temp.size(), 1);
        Assert.assertTrue(temp.get(0).contains(filename));

    }

    private void submitAndScheduleFeed(Bundle bundle) throws Exception {
        for (String cluster : bundle.getClusters()) {
            Util.assertSucceeded(prism.getClusterHelper().submitEntity(Util.URLS.SUBMIT_URL, cluster));
        }
        Util.assertSucceeded(prism.getFeedHelper().submitEntity(Util.URLS.SUBMIT_URL, bundle.getDataSets().get(0)));
        Util.assertSucceeded(prism.getFeedHelper().schedule(Util.URLS.SCHEDULE_URL, bundle.getDataSets().get(0)));
    }

    private void compareDataStoresForEquality(List<String> store1, List<String> store2) throws Exception {
        Assert.assertEquals(store1.size(), store2.size(), "DataStores are not equal!");
        Assert.assertTrue(Arrays.deepEquals(store2.toArray(new String[store2.size()]),
                store1.toArray(new String[store1.size()])), "DataStores are not equal!");
    }

    public HashMap<String, List<String>> getSystemState(ENTITY_TYPE entityType) throws Exception {
        IEntityManagerHelper prismHelper = prism.getClusterHelper();
        IEntityManagerHelper server1Helper = server1.getClusterHelper();
        IEntityManagerHelper server2Helper = server2.getClusterHelper();

        if (entityType.equals(ENTITY_TYPE.DATA)) {
            prismHelper = prism.getFeedHelper();
            server1Helper = server1.getFeedHelper();
            server2Helper = server2.getFeedHelper();
        }

        if (entityType.equals(ENTITY_TYPE.PROCESS)) {
            prismHelper = prism.getProcessHelper();
            server1Helper = server1.getProcessHelper();
            server2Helper = server2.getProcessHelper();
        }

        HashMap<String, List<String>> temp = new HashMap<String, List<String>>();
        temp.put("prismArchive", prismHelper.getArchiveInfo());
        temp.put("prismStore", prismHelper.getStoreInfo());
        temp.put("Server1Archive", server1Helper.getArchiveInfo());
        temp.put("Server1Store", server1Helper.getStoreInfo());
        temp.put("Server2Archive", server2Helper.getArchiveInfo());
        temp.put("Server2Store", server2Helper.getStoreInfo());

        return temp;
    }

}
