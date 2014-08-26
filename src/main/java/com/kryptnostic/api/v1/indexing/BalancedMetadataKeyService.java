package com.kryptnostic.api.v1.indexing;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import cern.colt.bitvector.BitVector;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.kryptnostic.api.v1.indexing.metadata.BalancedMetadata;
import com.kryptnostic.api.v1.indexing.metadata.BaseMetadatum;
import com.kryptnostic.api.v1.indexing.metadata.Metadata;
import com.kryptnostic.api.v1.indexing.metadata.Metadatum;
import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.linear.BitUtils;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

/**
 * @author Matthew Tamayo-Rios &lt;matthew@kryptnostic.com&gt; This class is used to compute the appropriate location to store
 *         a token's metadata.
 * 
 */
public class BalancedMetadataKeyService implements MetadataKeyService {
    private static final Random r = new SecureRandom();
    private final SimplePolynomialFunction hashFunction;
    private final HashFunction hf = Hashing.sha256();
    private final int bucketSize;
    private final int nonceLength;

    public BalancedMetadataKeyService(SimplePolynomialFunction hashFunction, int bucketSize, int nonceLength) {
        this.hashFunction = hashFunction;
        this.bucketSize = bucketSize;
        this.nonceLength = nonceLength;
    }

    public String getKey(String token, BitVector nonce) {
        BitVector tokenVector = Indexes.computeHashAndGetBits( hf , token );
        return BitVectors.marshalBitvector(hashFunction.apply(tokenVector, nonce));
    }

    @Override
    public Metadata mapTokensToKeys(Set<Metadatum> metadata) {
        /*
         * Let's balance the metadatum set and generate random nonces. Generally, the list of metadatum should be of
         * length one, but in rare cases collisions may occur. In the case of a collision we'll just store both at the
         * same location. In the future, we may want to have a specific number of retries before giving up and allowing
         * a collision. In theory this shouldn't be a security risk, because its hard for an attacker to force stuff
         * into the same bucket, unless they compromise the random number generator.
         */
        Map<String, List<Metadatum>> metadataMap = Maps.newHashMapWithExpectedSize(metadata.size());
        List<BitVector> nonces = Lists.newArrayList();
        for (Metadatum metadatum : metadata) {
            String token = metadatum.getToken();
            List<Integer> locations = metadatum.getLocations();
            int fromIndex = 0, toIndex = Math.min(locations.size(), bucketSize);
            do {
                Metadatum balancedMetadatum = new BaseMetadatum(metadatum.getDocumentId(), token, subListAndPad(
                        locations, fromIndex, toIndex));
                BitVector nonce = BitUtils.randomVector(nonceLength);
                String key = getKey(token, nonce);
                nonces.add(nonce);
                List<Metadatum> metadatumList = metadataMap.get(key);
                // TODO: Retry a few times instead of just allowing collision.
                if (metadatumList == null) {
                    metadatumList = Lists.newArrayListWithExpectedSize(1);
                    metadataMap.put(key, metadatumList);
                }
                metadatumList.add(balancedMetadatum);
                fromIndex += bucketSize;
                toIndex += bucketSize;
                if (toIndex > locations.size()) {
                    toIndex = locations.size();
                }
            } while (fromIndex < toIndex);
        }
        return BalancedMetadata.from(metadataMap, nonces);
    }

    private Iterable<Integer> subListAndPad(List<Integer> locations, int fromIndex, int toIndex) {
        int paddingLength = bucketSize - toIndex + fromIndex;
        List<Integer> padding = Lists.newArrayListWithCapacity(paddingLength);
        for (int i = 0; i < paddingLength; ++i) {
            int invalidLocation = r.nextInt();
            padding.add(invalidLocation < 0 ? invalidLocation : -invalidLocation);
        }

        return Iterables.concat(locations.subList(fromIndex, toIndex), padding);
    }
}
