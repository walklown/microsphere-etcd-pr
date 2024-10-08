/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.microsphere.etcd.spring.cloud.client.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KeyValue;
import io.microsphere.etcd.spring.cloud.client.EtcdClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.List;
import java.util.stream.Collectors;

import static io.microsphere.etcd.spring.cloud.client.util.KVClientUtils.resolveServiceId;
import static io.microsphere.etcd.spring.cloud.client.util.KVClientUtils.toByteSequence;

/**
 * Spring Cloud {@link DiscoveryClient} for etcd
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see DiscoveryClient
 * @since 1.0.0
 */
public class EtcdDiscoveryClient implements DiscoveryClient, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(EtcdDiscoveryClient.class);

    private final EtcdClientProperties etcdClientProperties;

    private final ServiceInstancesCache serviceInstancesCache;

    private final EtcdClientAdapter etcdClientAdapter;

    public EtcdDiscoveryClient(Client client, EtcdClientProperties etcdClientProperties, ObjectMapper objectMapper) {
        this.etcdClientAdapter = new EtcdClientAdapter(client, objectMapper);
        this.etcdClientProperties = etcdClientProperties;
        this.serviceInstancesCache = new ServiceInstancesCache(etcdClientProperties.getRootPath(), etcdClientAdapter);
    }

    @Override
    public String description() {
        return "Spring Cloud DiscoveryClient for etcd";
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceId) {
        return serviceInstancesCache.getValue(serviceId);
    }

    @Override
    public List<String> getServices() {
        String rootPath = etcdClientProperties.getRootPath();
        ByteSequence key = toByteSequence(rootPath);
        List<KeyValue> keyValues = etcdClientAdapter.getKeyValues(key, true);
        return keyValues.stream().map(KeyValue::getKey)
                .map(ByteSequence::toString)
                .map(path -> resolveServiceId(path, rootPath))
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public void probe() {
        // TODO

    }

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public void destroy() throws Exception {
        this.etcdClientAdapter.close();
    }
}
