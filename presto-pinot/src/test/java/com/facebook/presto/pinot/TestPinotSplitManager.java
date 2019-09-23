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

import com.facebook.presto.spi.ColumnHandle;
import com.facebook.presto.spi.predicate.Domain;
import com.facebook.presto.spi.predicate.EquatableValueSet;
import com.facebook.presto.spi.predicate.Range;
import com.facebook.presto.spi.predicate.TupleDomain;
import com.facebook.presto.spi.predicate.ValueSet;
import com.facebook.presto.testing.assertions.Assert;
import io.airlift.slice.Slices;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static com.facebook.presto.spi.type.BigintType.BIGINT;
import static com.facebook.presto.spi.type.VarcharType.VARCHAR;
import static com.facebook.presto.type.ColorType.COLOR;

public class TestPinotSplitManager
{
    private static final PinotSplitManager pinotSplitManager = new PinotSplitManager(new PinotConnection(new PinotClusterInfoFetcher(new PinotConfig())));
    private static final PinotColumnHandle columnCityId = new PinotColumnHandle("city_id", BIGINT, 0);
    private static final PinotColumnHandle columnCountryName = new PinotColumnHandle("country_name", VARCHAR, 1);
    private static final PinotColumnHandle columnColor = new PinotColumnHandle("color", COLOR, 2);

    @Test
    public void testMultipleColumns()
    {
        Domain domain1 = Domain.create(ValueSet.ofRanges(
                Range.lessThan(BIGINT, 10L)), false);

        Domain domain2 = Domain.create(ValueSet.ofRanges(
                Range.equal(VARCHAR, Slices.utf8Slice("cn")),
                Range.equal(VARCHAR, Slices.utf8Slice("us"))), false);

        Map<ColumnHandle, Domain> domainMap = new HashMap<>();
        domainMap.put(columnCityId, domain1);
        domainMap.put(columnCountryName, domain2);
        TupleDomain<ColumnHandle> constraintSummary = TupleDomain.withColumnDomains(domainMap);

        String expectedFilter = "((city_id < 10)) AND (country_name IN (\"cn\",\"us\"))";
        Assert.assertEquals(expectedFilter, pinotSplitManager.getPinotPredicate(constraintSummary));
    }

    @Test
    public void testNegativeDiscreteValues()
    {
        HashSet<EquatableValueSet.ValueEntry> set = new HashSet<>();
        set.add(EquatableValueSet.ValueEntry.create(COLOR, 1L));
        set.add(EquatableValueSet.ValueEntry.create(COLOR, 2L));
        Domain domain1 = Domain.create(new EquatableValueSet(COLOR, false, set), false);
        Map<ColumnHandle, Domain> domainMap = new HashMap<>();
        domainMap.put(columnColor, domain1);
        TupleDomain<ColumnHandle> constraintSummary = TupleDomain.withColumnDomains(domainMap);
        String expectedFilter = "(color NOT IN (1,2))";
        Assert.assertEquals(expectedFilter, pinotSplitManager.getPinotPredicate(constraintSummary));
    }

    /**
     * Test NOT predicate. Note that types currently supported by Pinot are all orderable,
     * so discrete values would appear as single values in the ranges
     * <p>
     * In the test below, the original predicate is WHERE city_id NOT IN (1, 10).
     * - The TupleDomain passed to Pinot is the allowed ranges, and instead of discrete values
     * - So the final translated predicate would be the union of (-Infinity, 1), (1, 10), (10, Infinity)
     * - It might not look as clean as the original predicate, but is still accurate
     */
    @Test
    public void testNotPredicateInRanges()
    {
        Domain domain1 = Domain.create(ValueSet.ofRanges(
                Range.lessThan(BIGINT, 1L),
                Range.greaterThan(BIGINT, 1L).intersect(Range.lessThan(BIGINT, 10L)),
                Range.greaterThan(BIGINT, 10L)), false);

        Map<ColumnHandle, Domain> domainMap = new HashMap<>();
        domainMap.put(columnCityId, domain1);
        TupleDomain<ColumnHandle> constraintSummary = TupleDomain.withColumnDomains(domainMap);

        String expectedFilter = "((city_id < 1) OR (1 < city_id AND city_id < 10) OR (10 < city_id))";
        Assert.assertEquals(expectedFilter, pinotSplitManager.getPinotPredicate(constraintSummary));
    }

    @Test
    public void testEmptyConstraintSummary()
    {
        Assert.assertEquals("", pinotSplitManager.getPinotPredicate(TupleDomain.all()));
    }
}