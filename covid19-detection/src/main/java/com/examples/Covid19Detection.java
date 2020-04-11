/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.examples;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.util.BufferedImageUtils;
import ai.djl.modality.cv.util.NDImageUtils;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Covid19Detection {
    private static final Logger logger = LoggerFactory.getLogger(Covid19Detection.class);

    public static void main(String[] args)
            throws IOException, MalformedModelException, TranslateException,
                    ModelNotFoundException {
        String imagePath = args[0];
        Criteria.Builder<BufferedImage, Classifications> builder =
                Criteria.builder()
                        .setTypes(BufferedImage.class, Classifications.class)
                        .optApplication(Application.UNDEFINED)
                        .optTranslator(new MyTranslator());

        try (ZooModel<BufferedImage, Classifications> model = ModelZoo.loadModel(builder.build())) {
            try (Predictor<BufferedImage, Classifications> predictor = model.newPredictor()) {
                Classifications result =
                        predictor.predict(BufferedImageUtils.fromFile(Paths.get(imagePath)));
                logger.info("Diagnose:");
                logger.info(result.toString());
            }
        }
    }

    private static final class MyTranslator implements Translator<BufferedImage, Classifications> {

        private List<String> classes;

        public MyTranslator() {
            classes = Arrays.asList("covid-19", "normal");
        }

        @Override
        public NDList processInput(TranslatorContext ctx, BufferedImage input) {
            NDArray array =
                    BufferedImageUtils.toNDArray(
                            ctx.getNDManager(), input, NDImageUtils.Flag.COLOR);
            array = NDImageUtils.resize(array, 224).div(255.0f);
            return new NDList(array);
        }

        @Override
        public Classifications processOutput(TranslatorContext ctx, NDList list) {
            NDArray probabilities = list.singletonOrThrow();
            return new Classifications(classes, probabilities);
        }
    }
}
