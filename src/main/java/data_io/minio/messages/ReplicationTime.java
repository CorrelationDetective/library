/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2020 MinIO, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package data_io.minio.messages;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import javax.annotation.Nonnull;
import java.util.Objects;

/** Helper class to denote replication time information for {@link ReplicationDestination}. */
@Root(name = "ReplicationTime")
public class ReplicationTime {
  @Element(name = "Time")
  private ReplicationTimeValue time;

  @Element(name = "Status")
  private Status status;

  public ReplicationTime(
      @Nonnull @Element(name = "Time") ReplicationTimeValue time,
      @Nonnull @Element(name = "Status") Status status) {
    this.time = Objects.requireNonNull(time, "Time must not be null");
    this.status = Objects.requireNonNull(status, "Status must not be null");
  }

  public ReplicationTimeValue time() {
    return this.time;
  }

  public Status status() {
    return this.status;
  }
}
