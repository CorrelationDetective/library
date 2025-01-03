/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2019 MinIO, Inc.
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
import org.simpleframework.xml.Root;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Object representation of request XML of <a
 * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_SelectObjectContent.html">SelectObjectContent
 * API</a>.
 */
@Root(name = "SelectObjectContentRequest")
@Namespace(reference = "http://s3.amazonaws.com/doc/2006-03-01/")

public class SelectObjectContentRequest extends SelectObjectContentRequestBase {
  @Element(name = "RequestProgress", required = false)
  private RequestProgress requestProgress;

  @Element(name = "ScanRange", required = false)
  private ScanRange scanRange;

  /** Constructs new SelectObjectContentRequest object for given parameters. */
  public SelectObjectContentRequest(
      @Nonnull String expression,
      boolean requestProgress,
      @Nonnull InputSerialization is,
      @Nonnull OutputSerialization os,
      @Nullable Long scanStartRange,
      @Nullable Long scanEndRange) {
    super(expression, is, os);
    if (requestProgress) {
      this.requestProgress = new RequestProgress();
    }
    if (scanStartRange != null || scanEndRange != null) {
      this.scanRange = new ScanRange(scanStartRange, scanEndRange);
    }
  }
}
