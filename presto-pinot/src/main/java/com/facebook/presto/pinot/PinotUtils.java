/*
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
package com.facebook.presto.pinot;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

final class PinotUtils
{
    private PinotUtils()
    {
    }

    public static final String TIME_COLUMN_NAME = "timeColumnName";
    public static final String TIME_COLUMN_VALUE = "timeColumnValue";
    public static final String QUOTE = "\"";

    public static <A, B extends A> B checkType(A value, Class<B> target, String name)
    {
        checkArgument(target.isInstance(requireNonNull(value)), "%s must be of type %s, not %s", name, target.getName(), value.getClass().getName());
        return target.cast(value);
    }
}