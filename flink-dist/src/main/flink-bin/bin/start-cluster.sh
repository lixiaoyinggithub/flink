#!/usr/bin/env bash
################################################################################
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
# limitations under the License.
################################################################################

bin=`dirname "$0"`
# 获得路径
bin=`cd "$bin"; pwd`
# 先加载config
. "$bin"/config.sh

# Start the JobManager instance(s)
shopt -s nocasematch
if [[ $HIGH_AVAILABILITY == "zookeeper" ]]; then
    # HA Mode 高可用模式调用readMasters方法 读取端口等
    readMasters

    #输出读取到的内容
    echo "Starting HA cluster with ${#MASTERS[@]} masters."

    for ((i=0;i<${#MASTERS[@]};++i)); do
        master=${MASTERS[i]}
        webuiport=${WEBUIPORTS[i]}

        # 本地模式
        if [ ${MASTERS_ALL_LOCALHOST} = true ] ; then
            # 最终的启动脚本在jobmanager
            "${FLINK_BIN_DIR}"/jobmanager.sh start "${master}" "${webuiport}"
        else
            ssh -n $FLINK_SSH_OPTS $master -- "nohup /bin/bash -l \"${FLINK_BIN_DIR}/jobmanager.sh\" start ${master} ${webuiport} &"
        fi
    done

else
    echo "Starting cluster."

    # Start single JobManager on this machine 在当前节点启动JobManager
    "$FLINK_BIN_DIR"/jobmanager.sh start
fi
shopt -u nocasematch

# Start TaskManager instance(s)
TMSlaves start
