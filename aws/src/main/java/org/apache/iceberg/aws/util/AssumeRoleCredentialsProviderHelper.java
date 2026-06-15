/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.iceberg.aws.util;

import java.util.UUID;

import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

public class AssumeRoleCredentialsProviderHelper {

    private AwsProperties awsProperties;
    private String roleSessionName;

    public AssumeRoleCredentialsProviderHelper(AwsProperties awsProperties) {
        this.awsProperties = awsProperties;
        this.roleSessionName = genSessionName();
        Preconditions.checkNotNull(
                awsProperties.clientAssumeRoleArn(),
                "Cannot initialize AssumeRoleClientConfigFactory with null role ARN");
        Preconditions.checkNotNull(
                awsProperties.clientAssumeRoleRegion(),
                "Cannot initialize AssumeRoleClientConfigFactory with null region");
    }

    public AssumeRoleCredentialsProviderHelper(AwsProperties awsProperties, String roleSessionName) {
        this.awsProperties = awsProperties;
        this.roleSessionName = roleSessionName;
        Preconditions.checkNotNull(
                awsProperties.clientAssumeRoleArn(),
                "Cannot initialize AssumeRoleClientConfigFactory with null role ARN");
        Preconditions.checkNotNull(
                awsProperties.clientAssumeRoleRegion(),
                "Cannot initialize AssumeRoleClientConfigFactory with null region");
    }

    private StsClient sts() {
        return StsClient.builder()
                .applyMutation(httpClientProperties::applyHttpClientConfigurations)
                .build();
    }

    private String genSessionName() {
        if (awsProperties.clientAssumeRoleSessionName() != null) {
            return awsProperties.clientAssumeRoleSessionName();
        }

        return String.format("iceberg-aws-%s", UUID.randomUUID());
    }

    public AssumeRoleCredentialsProvider createCredentialsProvider() {
        return StsAssumeRoleCredentialsProvider.builder()
                .stsClient(sts())
                .refreshRequest(createAssumeRoleRequest())
                .build();
    }

    private AssumeRoleRequest createAssumeRoleRequest() {
        return AssumeRoleRequest.builder()
                .roleArn(awsProperties.clientAssumeRoleArn())
                .roleSessionName(roleSessionName)
                .durationSeconds(awsProperties.clientAssumeRoleTimeoutSec())
                .externalId(awsProperties.clientAssumeRoleExternalId())
                .tags(awsProperties.stsClientAssumeRoleTags())
                .build();
    }
}
