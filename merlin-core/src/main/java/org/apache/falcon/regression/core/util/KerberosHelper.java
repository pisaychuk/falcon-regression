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

import org.apache.falcon.regression.core.MerlinConstants;
import org.testng.Assert;
import org.testng.log4testng.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class KerberosHelper {

    private static Logger logger = Logger.getLogger(KerberosHelper.class);

    private static String currentKerberosUser = null;

    public static void switchUser(String user) {
        if(!MerlinConstants.IS_SECURE) {
            logger.info("Kerberos is disabled, hence no user switching.");
            return;
        }

        if(user == null) {
            user = MerlinConstants.CURRENT_USER_NAME;
        }

        //for the first call kerberos switching happens as the currentKerberosUser is null
        if(user.equals(currentKerberosUser)) {
            logger.info("kerberos switching is not required.");
            return;
        }
        final String keytab = MerlinConstants.getKeytabForUser(user);
        logger.info(String.format("Switching kerberos keytab from %s to %s", currentKerberosUser, user));
        final String command = String.format("kinit -kt %s %s", keytab, user);
        final int exitVal = executeCommand(command);
        Assert.assertEquals(exitVal, 0, "Switching Kerberos credential did not succeed.");
        currentKerberosUser = user;
    }
    /* Example usage:
    String command = "ping -c 3 google.com";
    String output = executeCommand(command);
    System.out.println(output);
    */
    private static int executeCommand(String command) {
        StringBuilder output = new StringBuilder();
        StringBuilder error = new StringBuilder();

        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader outputReader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader errorReader =
                    new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String line = "";
            while ((line = outputReader.readLine()) != null) {
                output.append(line + "\n");
            }
            while ((line = errorReader.readLine()) != null) {
                error.append(line + "\n");
            }
            logger.info("output:" + output.toString());
            logger.info("error:" + error.toString());
            logger.info("exit status:" + p.exitValue());
            return p.exitValue();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }


}