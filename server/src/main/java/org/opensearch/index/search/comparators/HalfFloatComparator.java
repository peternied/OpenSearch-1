/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.index.search.comparators;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.sandbox.document.HalfFloatPoint;
import org.apache.lucene.search.LeafFieldComparator;
import org.apache.lucene.search.comparators.NumericComparator;

import java.io.IOException;

/**
 * The comparator for half_float numeric type.
 * Comparator based on {@link Float#compare} for {@code numHits}. This comparator provides a
 * skipping functionality – an iterator that can skip over non-competitive documents.
 */
public class HalfFloatComparator extends NumericComparator<Float> {
    private final float[] values;
    protected float topValue;
    protected float bottom;

    public HalfFloatComparator(int numHits, String field, Float missingValue, boolean reverse, boolean enableSkipping) {
        super(field, missingValue != null ? missingValue : 0.0f, reverse, enableSkipping, HalfFloatPoint.BYTES);
        values = new float[numHits];
    }

    @Override
    public int compare(int slot1, int slot2) {
        return Float.compare(values[slot1], values[slot2]);
    }

    @Override
    public void setTopValue(Float value) {
        super.setTopValue(value);
        topValue = value;
    }

    @Override
    public Float value(int slot) {
        return Float.valueOf(values[slot]);
    }

    @Override
    public LeafFieldComparator getLeafComparator(LeafReaderContext context) throws IOException {
        return new HalfFloatLeafComparator(context);
    }

    /** Leaf comparator for {@link HalfFloatComparator} that provides skipping functionality */
    public class HalfFloatLeafComparator extends NumericLeafComparator {

        public HalfFloatLeafComparator(LeafReaderContext context) throws IOException {
            super(context);
        }

        private float getValueForDoc(int doc) throws IOException {
            if (docValues.advanceExact(doc)) {
                return Float.intBitsToFloat((int) docValues.longValue());
            } else {
                return missingValue;
            }
        }

        @Override
        public void setBottom(int slot) throws IOException {
            bottom = values[slot];
            super.setBottom(slot);
        }

        @Override
        public int compareBottom(int doc) throws IOException {
            return Float.compare(bottom, getValueForDoc(doc));
        }

        @Override
        public int compareTop(int doc) throws IOException {
            return Float.compare(topValue, getValueForDoc(doc));
        }

        @Override
        public void copy(int slot, int doc) throws IOException {
            values[slot] = getValueForDoc(doc);
            super.copy(slot, doc);
        }

        @Override
        protected int compareMissingValueWithBottomValue() {
            return Float.compare(missingValue, bottom);
        }

        @Override
        protected int compareMissingValueWithTopValue() {
            return Float.compare(missingValue, topValue);
        }

        @Override
        protected void encodeBottom(byte[] packedValue) {
            HalfFloatPoint.encodeDimension(bottom, packedValue, 0);
        }

        @Override
        protected void encodeTop(byte[] packedValue) {
            HalfFloatPoint.encodeDimension(topValue, packedValue, 0);
        }
    }
}
