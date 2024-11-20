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

/** Helper class to denote tag information for {@link RuleFilter}. */
@Root(name = "Tag")
public class Tag {
  @Element(name = "Key")
  private String key;

  @Element(name = "Value")
  private String value;

  public Tag(
      @Nonnull @Element(name = "Key") String key, @Nonnull @Element(name = "Value") String value) {
    Objects.requireNonNull(key, "Key must not be null");
    if (key.isEmpty()) {
      throw new IllegalArgumentException("Key must not be empty");
    }

    this.key = key;
    this.value = Objects.requireNonNull(value, "Value must not be null");
  }

  public String key() {
    return this.key;
  }

  public String value() {
    return this.value;
  }
}
