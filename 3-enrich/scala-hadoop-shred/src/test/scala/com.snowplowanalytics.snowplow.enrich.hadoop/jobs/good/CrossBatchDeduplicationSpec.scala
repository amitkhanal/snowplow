/*
 * Copyright (c) 2012-2017 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.snowplow.enrich
package hadoop
package jobs
package good

// This project
import JobSpecHelpers._

// Specs2
import org.specs2.mutable.Specification

/**
 * Holds the input data for the test, plus the expected output.
 */
object CrossBatchDeduplicationSpec {

  // original duplicated event_id
  val dupeUuid = "1799a90f-f570-4414-b91a-b0db8f39cc2e"
  val dupeFp = "bed9a39a0917874d2ff072033a6413d8"

  val uniqueUuid = "e271698a-3e86-4b2f-bb1b-f9f7aa5666c1"
  val uniqueFp = "e79bef64f3185e9d7c10d5dfdf27b9a3"

  val inbatchDupeUuid = "2718ac0f-f510-4314-a98a-cfdb8f39abe4"
  val inbatchDupeFp = "aba1c39a091787aa231072033a647caa"

  // ETL Timestamps
  val previousEtlTstamp = "2016-11-27 06:46:10.000"   // 1480203970
  val currentEtlTstamp = "2016-11-27 08:46:40.000"    // 1480211200

  object Storage {

    /**
      * Helper container class to hold components stored in DuplicationStorage
      */
    case class DuplicateTriple(eventId: String, eventFingerprint: String, etlTstamp: String)

    // Events processed in previous runs
    val dupeStorage = List(
      // Event stored during last ETL, which duplicate will be present
      DuplicateTriple(dupeUuid, dupeFp, previousEtlTstamp),
      // Same payload, but unique id
      DuplicateTriple("randomUuid", dupeFp, previousEtlTstamp),
      // Synthetic duplicate
      DuplicateTriple(dupeUuid, "randomFp", previousEtlTstamp),
      // Event written during last (failed) ETL
      DuplicateTriple(uniqueUuid, uniqueFp, currentEtlTstamp)
    )

    /**
      * Delete and re-create local DynamoDB table designed to store duplicate triples
      * Also add initial duplicate from `dupeStorage`
      */
    def prepareLocalTable() = {
      val dupesTable = "dupes"
      val client = DuplicateStorage.DynamoDbStorage.getLocalClient
      client.deleteTable(dupesTable)
      val table = DuplicateStorage.DynamoDbStorage.createTable(client, dupesTable).table
      dupeStorage.foreach { case DuplicateTriple(eid, fp, etlTstamp) =>
        client.putItem(table.name, "eventId" -> eid, "fingerprint" -> fp, "etlTime" -> etlTstamp)
      }
    }
  }

  // Events, including one cross-batch duplicate and in-batch duplicates
  val lines = Lines(
    // In-batch unique event that has natural duplicate in dupe storage
    s"""blog	web	$currentEtlTstamp	2016-11-27 07:16:07.000	2016-11-27 07:16:07.333	page_view	$dupeUuid		blogTracker	js-2.7.0-rc2	clj-1.1.0-tom-0.2.0	hadoop-1.8.0-common-0.24.0		185.124.153.x	531497290	1f9b3980-6619-4d75-a6c9-8253c76c3bfb	18	5beb1f92-d4fb-4020-905c-f659929c8ab5												http://chuwy.me/scala-blocks.html	Scala Code Blocks	http://chuwy.me/	http	chuwy.me	80	/scala-blocks.html			http	chuwy.me	80	/			internal																																	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.98 Safari/537.36	Chrome	Chrome	54.0.2840.98	Browser	WEBKIT		1	1	0	0	0	0	0	0	0	1	24	1280	726	Mac OS X	Mac OS X	Apple Inc.	Asia/Omsk	Computer	0	1280	800	UTF-8	1280	4315												2016-11-27 07:16:07.340			{"schema":"iglu:com.snowplowanalytics.snowplow/contexts/jsonschema/1-0-1","data":[{"schema":"iglu:com.snowplowanalytics.snowplow/ua_parser_context/jsonschema/1-0-0","data":{"useragentFamily":"Chrome","useragentMajor":"54","useragentMinor":"0","useragentPatch":"2840","useragentVersion":"Chrome 54.0.2840","osFamily":"MacOS X","osMajor":"10","osMinor":"11","osPatch":"6","osPatchMinor":null,"osVersion":"Mac OS X 10.11.6","deviceFamily":"Other"}}]}	395e4506-37a3-4074-8de2-d8c75fb17d4a	2016-11-27 07:16:06.993	com.snowplowanalytics.snowplow	page_view	jsonschema	1-0-0	$dupeFp	""",

    // In-batch natural duplicates
    s"""blog	web	$currentEtlTstamp	2016-11-27 06:26:17.000	2016-11-27 06:26:17.333	page_view	$inbatchDupeUuid		blogTracker	js-2.7.0-rc2	clj-1.1.0-tom-0.2.0	hadoop-1.8.0-common-0.24.0		185.124.153.x	531497290	1f9b3980-6619-4d75-a6c9-8253c76c3bfb	18	5beb1f92-d4fb-4020-905c-f659929c8ab5												http://chuwy.me/scala-blocks.html	Scala Code Blocks	http://chuwy.me/	http	chuwy.me	80	/scala-blocks.html			http	chuwy.me	80	/			internal																																	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.98 Safari/537.36	Chrome	Chrome	54.0.2840.98	Browser	WEBKIT		1	1	0	0	0	0	0	0	0	1	24	1280	726	Mac OS X	Mac OS X	Apple Inc.	Asia/Omsk	Computer	0	1280	800	UTF-8	1280	4315												2016-11-27 07:16:07.340			{"schema":"iglu:com.snowplowanalytics.snowplow/contexts/jsonschema/1-0-1","data":[{"schema":"iglu:com.snowplowanalytics.snowplow/ua_parser_context/jsonschema/1-0-0","data":{"useragentFamily":"Chrome","useragentMajor":"54","useragentMinor":"0","useragentPatch":"2840","useragentVersion":"Chrome 54.0.2840","osFamily":"MacOS X","osMajor":"10","osMinor":"11","osPatch":"6","osPatchMinor":null,"osVersion":"Mac OS X 10.11.6","deviceFamily":"Other"}}]}	395e4506-37a3-4074-8de2-d8c75fb17d4a	2016-11-27 07:16:06.993	com.snowplowanalytics.snowplow	page_view	jsonschema	1-0-0	$inbatchDupeFp	""",
    s"""blog	web	$currentEtlTstamp	2016-11-27 06:26:17.000	2016-11-27 06:26:17.333	page_view	$inbatchDupeUuid		blogTracker	js-2.7.0-rc2	clj-1.1.0-tom-0.2.0	hadoop-1.8.0-common-0.24.0		185.124.153.x	531497290	1f9b3980-6619-4d75-a6c9-8253c76c3bfb	18	5beb1f92-d4fb-4020-905c-f659929c8ab5												http://chuwy.me/scala-blocks.html	Scala Code Blocks	http://chuwy.me/	http	chuwy.me	80	/scala-blocks.html			http	chuwy.me	80	/			internal																																	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.98 Safari/537.36	Chrome	Chrome	54.0.2840.98	Browser	WEBKIT		1	1	0	0	0	0	0	0	0	1	24	1280	726	Mac OS X	Mac OS X	Apple Inc.	Asia/Omsk	Computer	0	1280	800	UTF-8	1280	4315												2016-11-27 07:16:07.340			{"schema":"iglu:com.snowplowanalytics.snowplow/contexts/jsonschema/1-0-1","data":[{"schema":"iglu:com.snowplowanalytics.snowplow/ua_parser_context/jsonschema/1-0-0","data":{"useragentFamily":"Chrome","useragentMajor":"54","useragentMinor":"0","useragentPatch":"2840","useragentVersion":"Chrome 54.0.2840","osFamily":"MacOS X","osMajor":"10","osMinor":"11","osPatch":"6","osPatchMinor":null,"osVersion":"Mac OS X 10.11.6","deviceFamily":"Other"}}]}	395e4506-37a3-4074-8de2-d8c75fb17d4a	2016-11-27 07:16:06.993	com.snowplowanalytics.snowplow	page_view	jsonschema	1-0-0	$inbatchDupeFp	""",

    // Fully unique event
    s"""blog	web	$currentEtlTstamp	2016-11-27 18:12:17.000	2016-11-27 17:00:01.333	page_view	$uniqueUuid		blogTracker	js-2.7.0-rc2	clj-1.1.0-tom-0.2.0	hadoop-1.8.0-common-0.24.0		199.124.153.x	531497290	1f9b3980-6619-4d75-a6c9-8253c76c3bfb	18	5beb1f92-d4fb-4020-905c-f659929c8ab5												http://chuwy.me/scala-blocks.html	Scala Code Blocks	http://chuwy.me/	http	chuwy.me	80	/scala-blocks.html			http	chuwy.me	80	/			internal																																	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.98 Safari/537.36	Chrome	Chrome	54.0.2840.98	Browser	WEBKIT		1	1	0	0	0	0	0	0	0	1	24	1280	726	Mac OS X	Mac OS X	Apple Inc.	Asia/Omsk	Computer	0	1280	800	UTF-8	1280	4315												2016-11-27 07:16:07.340			{"schema":"iglu:com.snowplowanalytics.snowplow/contexts/jsonschema/1-0-1","data":[{"schema":"iglu:com.snowplowanalytics.snowplow/ua_parser_context/jsonschema/1-0-0","data":{"useragentFamily":"Chrome","useragentMajor":"54","useragentMinor":"0","useragentPatch":"2840","useragentVersion":"Chrome 54.0.2840","osFamily":"MacOS X","osMajor":"10","osMinor":"11","osPatch":"6","osPatchMinor":null,"osVersion":"Mac OS X 10.11.6","deviceFamily":"Other"}}]}	395e4506-37a3-4074-8de2-d8c75fb17d4a	2016-11-27 07:16:06.993	com.snowplowanalytics.snowplow	page_view	jsonschema	1-0-0	$uniqueFp	"""
  )

  object expected {
    val path = "com.snowplowanalytics.snowplow/duplicate/jsonschema/1-0-0"

    val additionalContextPath = "com.snowplowanalytics.snowplow/ua_parser_context/jsonschema/1-0-0"
    val additionalContextContents1 =
      s"""
        |{
        |"schema":{
          |"vendor":"com.snowplowanalytics.snowplow",
          |"name":"ua_parser_context",
          |"format":"jsonschema",
          |"version":"1-0-0"
        |},
        |"data":{
          |"useragentFamily":"Chrome",
          |"useragentMajor":"54",
          |"useragentMinor":"0",
          |"useragentPatch":"2840",
          |"useragentVersion":"Chrome 54.0.2840",
          |"osFamily":"MacOS X",
          |"osMajor":"10",
          |"osMinor":"11",
          |"osPatch":"6",
          |"osPatchMinor":null,
          |"osVersion":"Mac OS X 10.11.6",
          |"deviceFamily":"Other"
        |},
        |"hierarchy":{
          |"rootId":"$uniqueUuid",
          |"rootTstamp":"2016-11-27 18:12:17.000",
          |"refRoot":"events",
          |"refTree":["events","ua_parser_context"],
          |"refParent":"events"
        |}
        |}""".stripMargin.replaceAll("[\n\r]","")

    val additionalContextContents2 =
      s"""
         |{
         |"schema":{
         |"vendor":"com.snowplowanalytics.snowplow",
         |"name":"ua_parser_context",
         |"format":"jsonschema",
         |"version":"1-0-0"
         |},
         |"data":{
         |"useragentFamily":"Chrome",
         |"useragentMajor":"54",
         |"useragentMinor":"0",
         |"useragentPatch":"2840",
         |"useragentVersion":"Chrome 54.0.2840",
         |"osFamily":"MacOS X",
         |"osMajor":"10",
         |"osMinor":"11",
         |"osPatch":"6",
         |"osPatchMinor":null,
         |"osVersion":"Mac OS X 10.11.6",
         |"deviceFamily":"Other"
         |},
         |"hierarchy":{
         |"rootId":"$inbatchDupeUuid",
         |"rootTstamp":"2016-11-27 06:26:17.000",
         |"refRoot":"events",
         |"refTree":["events","ua_parser_context"],
         |"refParent":"events"
         |}
         |}""".stripMargin.replaceAll("[\n\r]","")

    val events = List(
      s"""blog	web	$currentEtlTstamp	2016-11-27 06:26:17.000	2016-11-27 06:26:17.333	page_view	$inbatchDupeUuid		blogTracker	js-2.7.0-rc2	clj-1.1.0-tom-0.2.0	hadoop-1.8.0-common-0.24.0		185.124.153.x	531497290	1f9b3980-6619-4d75-a6c9-8253c76c3bfb	18	5beb1f92-d4fb-4020-905c-f659929c8ab5												http://chuwy.me/scala-blocks.html	Scala Code Blocks	http://chuwy.me/	http	chuwy.me	80	/scala-blocks.html			http	chuwy.me	80	/			internal																															Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.98 Safari/537.36	Chrome	Chrome	54.0.2840.98	Browser	WEBKIT		1	1	0	0	0	0	0	0	0	1	24	1280	726	Mac OS X	Mac OS X	Apple Inc.	Asia/Omsk	Computer	0	1280	800	UTF-8	1280	4315												2016-11-27 07:16:07.340			395e4506-37a3-4074-8de2-d8c75fb17d4a	2016-11-27 07:16:06.993	com.snowplowanalytics.snowplow	page_view	jsonschema	1-0-0	$inbatchDupeFp	""",
      s"""blog	web	$currentEtlTstamp	2016-11-27 18:12:17.000	2016-11-27 17:00:01.333	page_view	$uniqueUuid		blogTracker	js-2.7.0-rc2	clj-1.1.0-tom-0.2.0	hadoop-1.8.0-common-0.24.0		199.124.153.x	531497290	1f9b3980-6619-4d75-a6c9-8253c76c3bfb	18	5beb1f92-d4fb-4020-905c-f659929c8ab5												http://chuwy.me/scala-blocks.html	Scala Code Blocks	http://chuwy.me/	http	chuwy.me	80	/scala-blocks.html			http	chuwy.me	80	/			internal																															Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.98 Safari/537.36	Chrome	Chrome	54.0.2840.98	Browser	WEBKIT		1	1	0	0	0	0	0	0	0	1	24	1280	726	Mac OS X	Mac OS X	Apple Inc.	Asia/Omsk	Computer	0	1280	800	UTF-8	1280	4315												2016-11-27 07:16:07.340			395e4506-37a3-4074-8de2-d8c75fb17d4a	2016-11-27 07:16:06.993	com.snowplowanalytics.snowplow	page_view	jsonschema	1-0-0	$uniqueFp	"""
    )
  }

  /**
    * This assumes that local DynamoDB instance is running on 127.0.0.1
    */
  private def continuousIntegration: Boolean = sys.env.get("CI") match {
    case Some("true") => true
    case _ =>
      println("WARNING! Test requires CI envvar to be set and DynamoDB local instance to be running")
      false
  }
}

/**
 * Integration test for the EtlJob:
 *
 * Two enriched events with same event id and different payload*
 * This test requires local DynamoDB instance, running on 127.0.0.1:8000
 * @see https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.html
 */
class CrossBatchDeduplicationSpec extends Specification {

  skipAllUnless(CrossBatchDeduplicationSpec.continuousIntegration)

  "A job which is provided with a two events with same event_id" should {

    CrossBatchDeduplicationSpec.Storage.prepareLocalTable()

    val Sinks =
      JobSpecHelpers.runJobInTool(CrossBatchDeduplicationSpec.lines)

    "remove cross-batch duplicate and store left event in /atomic-events" in {
      val lines = JobSpecHelpers.readFile(Sinks.output, "atomic-events")
      lines.sorted mustEqual CrossBatchDeduplicationSpec.expected.events
    }

    "shred two unique events out of cross-batch and in-batch duplicates" in {
      val lines = JobSpecHelpers.readFile(Sinks.output, "atomic-events")
      val eventIds = lines.map(_.split("\t").apply(6))
      eventIds.mustEqual(Seq(CrossBatchDeduplicationSpec.inbatchDupeUuid, CrossBatchDeduplicationSpec.uniqueUuid))
    }

    "shred additional contexts into their appropriate path" in {
      val contexts = JobSpecHelpers.readFile(Sinks.output, CrossBatchDeduplicationSpec.expected.additionalContextPath)
      contexts mustEqual Seq(CrossBatchDeduplicationSpec.expected.additionalContextContents2, CrossBatchDeduplicationSpec.expected.additionalContextContents1)
    }

    "not shred any unexpected JSONs" in {
      val expectedFiles = List("atomic-events", CrossBatchDeduplicationSpec.expected.path, CrossBatchDeduplicationSpec.expected.additionalContextPath)
      JobSpecHelpers.listFilesWithExclusions(Sinks.output, expectedFiles) must be empty
    }

    "not trap any exceptions" in {
      Sinks.exceptions must beEmptyFile
    }

    "not write any bad row JSONs" in {
      Sinks.badRows must beEmptyFile
    }

    Sinks.deleteAll()
  }
}
