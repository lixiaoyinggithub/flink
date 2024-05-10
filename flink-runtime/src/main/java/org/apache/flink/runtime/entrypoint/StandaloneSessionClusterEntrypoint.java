/*
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

package org.apache.flink.runtime.entrypoint;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.entrypoint.component.DispatcherResourceManagerComponentFactory;
import org.apache.flink.runtime.entrypoint.component.SessionDispatcherResourceManagerComponentFactory;
import org.apache.flink.runtime.entrypoint.parser.CommandLineParser;
import org.apache.flink.runtime.resourcemanager.StandaloneResourceManagerFactory;
import org.apache.flink.runtime.util.EnvironmentInformation;
import org.apache.flink.runtime.util.JvmShutdownSafeguard;
import org.apache.flink.runtime.util.SignalHandler;

/**
 * Entry point for the standalone session cluster.
 */
public class StandaloneSessionClusterEntrypoint extends SessionClusterEntrypoint {

	public StandaloneSessionClusterEntrypoint(Configuration configuration) {
		super(configuration);
	}

	@Override
	protected DispatcherResourceManagerComponentFactory<?> createDispatcherResourceManagerComponentFactory(Configuration configuration) {
		return new SessionDispatcherResourceManagerComponentFactory(StandaloneResourceManagerFactory.INSTANCE);
	}

	public static void main(String[] args) {
		// startup checks and logging
		EnvironmentInformation.logEnvironmentInfo(LOG, StandaloneSessionClusterEntrypoint.class.getSimpleName(), args);

		/**
		 * 注册信号处理程序，与操作系统相关
		 * 1、HUP中断信号：HUP中断信号的对应操作为让进程挂起，睡眠
		 * 2、INT中断信号：INT中断信号的对应操作为正常关闭所有进程
		 */
		SignalHandler.register(LOG);
		/**
		 * 注册钩子，当集群出现宕机或者停止的情况，执行服务的关闭操作
		 */
		JvmShutdownSafeguard.installAsShutdownHook(LOG);

		EntrypointClusterConfiguration entrypointClusterConfiguration = null;
		final CommandLineParser<EntrypointClusterConfiguration> commandLineParser = new CommandLineParser<>(new EntrypointClusterConfigurationParserFactory());

		try {
			/**
			 * 解析参数
			 */
			entrypointClusterConfiguration = commandLineParser.parse(args);
		} catch (FlinkParseException e) {
			LOG.error("Could not parse command line arguments {}.", args, e);
			commandLineParser.printHelp(StandaloneSessionClusterEntrypoint.class.getSimpleName());
			System.exit(1);
		}

		// 解析集群的核心配置文件
		Configuration configuration = loadConfiguration(entrypointClusterConfiguration);

		// 创建StandaloneSessionClusterEntrypoint和启动
		StandaloneSessionClusterEntrypoint entrypoint = new StandaloneSessionClusterEntrypoint(configuration);
		// 运行集群
		ClusterEntrypoint.runClusterEntrypoint(entrypoint);
	}
}
