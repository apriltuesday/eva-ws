/*
 * European Variation Archive (EVA) - Open-access database of all types of genetic
 * variation data from all species
 *
 * Copyright 2019 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package uk.ac.ebi.eva.server.ws.ga4gh;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.eva.commons.beacon.models.BeaconAlleleRequestBody;
import uk.ac.ebi.eva.commons.core.models.Region;
import uk.ac.ebi.eva.commons.core.models.VariantType;
import uk.ac.ebi.eva.commons.mongodb.entities.VariantMongo;
import uk.ac.ebi.eva.commons.mongodb.filter.FilterBuilder;
import uk.ac.ebi.eva.commons.mongodb.filter.VariantRepositoryFilter;
import uk.ac.ebi.eva.commons.mongodb.services.VariantWithSamplesAndAnnotationsService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GA4GHBeaconWSServerV2Test {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private VariantWithSamplesAndAnnotationsService service;

    @Before
    public void setup() throws Exception {
        VariantMongo variantMongo = new VariantMongo(null, "X", 100470026, 100470026, 1, "G", "A");
        List<VariantMongo> variantMongoList = Collections.singletonList(variantMongo);
        Region startRange = new Region("X", new Long(100470026), new Long(100470026));
        Region endRange = new Region("X", new Long(100470026), new Long(100470026));
        List<VariantRepositoryFilter> variantRepositoryFilters = new FilterBuilder().getBeaconFilters("G", "A", VariantType.SNV, Arrays.asList("PRJEB7218"));

        Pageable pageable = new PageRequest(0, 1000);
        given(service.findByRegionAndOtherBeaconFilters(eq(startRange), eq(endRange), eq(variantRepositoryFilters),
                eq(pageable))).willReturn(variantMongoList);
    }

    @Test
    public void testForExisting() throws Exception {
        BeaconAlleleRequestBody request = new BeaconAlleleRequestBody();
        request.setReferenceName("X");
        request.setAssemblyId("GRCh37");
        request.setReferenceBases("G");
        request.setStart(new Long(100470026));
        request.setEnd(new Long(100470026));
        request.setAlternateBases("A");
        request.setVariantType("SNV");
        request.setDatasetIds(Arrays.asList("PRJEB7218"));
        String url = String.format("/v2/beacon/query?referenceName=%s&referenceBases=%s&assemblyId=%s&alternateBases=" +
                        "%s&start=%s&end=%s&variantType=%s&datasetIds=%s",
                request.getReferenceName(),
                request.getReferenceBases(),
                request.getAssemblyId(),
                request.getAlternateBases(),
                request.getStart(),
                request.getEnd(),
                request.getVariantType(),
                String.join(",", request.getDatasetIds()));

        assertEquals(true, testBeaconHelper(url).getBody().getExists());

        request.setStartMin(new Long(1));
        request.setStartMax(new Long(1));
        request.setEndMin(new Long(1));
        request.setEndMax(new Long(1));
        url = String.format("/v2/beacon/query?referenceName=%s&referenceBases=%s&assemblyId=%s&" +
                        "alternateBases=%s&start=%s&end=%s&startMin=%s&endMin=%s&startMax=%s&endMax=%s&variantType=%s" +
                        "&datasetIds=%s",
                request.getReferenceName(),
                request.getReferenceBases(),
                request.getAssemblyId(),
                request.getAlternateBases(),
                request.getStart(),
                request.getEnd(),
                request.getStartMin(),
                request.getEndMin(),
                request.getStartMax(),
                request.getEndMax(),
                request.getVariantType(),
                String.join(",", request.getDatasetIds()));
        assertEquals(true, testBeaconHelper(url).getBody().getExists());
    }

    @Test
    public void testForNonExisting() {
        BeaconAlleleRequestBody request = new BeaconAlleleRequestBody();
        request.setReferenceName("Y");
        request.setAssemblyId("GRCh37");
        request.setReferenceBases("G");
        request.setAlternateBases("A");
        String url = String.format("/v2/beacon/query?referenceName=%s&referenceBases=%s&assemblyId=%s&" +
                        "alternateBases=%s",
                request.getReferenceName(),
                request.getReferenceBases(),
                request.getAssemblyId(),
                request.getAlternateBases());

        assertEquals(false, testBeaconHelper(url).getBody().getExists());
    }

    @Test
    public void testForError() {
        BeaconAlleleRequestBody request = new BeaconAlleleRequestBody();
        String url = String.format("/v2/beacon/query?referenceName=%s&referenceBases=%s&assemblyId=%s",
                "X",
                "G",
                "GRch37");
        assertEquals(400, testBeaconHelper(url).getBody().getError().getErrorCode());
        assertEquals("Either alternateBases or variantType is required",
                testBeaconHelper(url).getBody().getError().getErrorMessage());
    }

    private ResponseEntity<GA4GHBeaconQueryResponseV2> testBeaconHelper(String url) {

        ResponseEntity<GA4GHBeaconQueryResponseV2> response = restTemplate.getForEntity(
                url, GA4GHBeaconQueryResponseV2.class);
        return response;
    }
}
