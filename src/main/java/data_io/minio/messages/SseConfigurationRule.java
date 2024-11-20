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
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/** Helper class to denote Rule information for {@link SseConfiguration}. */
@Root(name = "Rule")
@Namespace(reference = "http://s3.amazonaws.com/doc/2006-03-01/")
public class SseConfigurationRule {
  @Path(value = "ApplyServerSideEncryptionByDefault")
  @Element(name = "KMSMasterKeyID", required = false)
  private String kmsMasterKeyId;

  @Path(value = "ApplyServerSideEncryptionByDefault")
  @Element(name = "SSEAlgorithm")
  private SseAlgorithm sseAlgorithm;

  /** Constructs new server-side encryption configuration rule. */
  public SseConfigurationRule(
      @Nonnull @Element(name = "SSEAlgorithm") SseAlgorithm sseAlgorithm,
      @Nullable @Element(name = "KMSMasterKeyID", required = false) String kmsMasterKeyId) {
    this.sseAlgorithm = Objects.requireNonNull(sseAlgorithm, "SSE Algorithm must be provided");
    this.kmsMasterKeyId = kmsMasterKeyId;
  }

  public String kmsMasterKeyId() {
    return this.kmsMasterKeyId;
  }

  public SseAlgorithm sseAlgorithm() {
    return this.sseAlgorithm;
  }
}
