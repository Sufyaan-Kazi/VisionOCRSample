/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.vision;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.vision.v1.AnnotateFileResponse;
import com.google.cloud.vision.v1.AnnotateFileResponse.Builder;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.AsyncAnnotateFileRequest;
import com.google.cloud.vision.v1.AsyncAnnotateFileResponse;
import com.google.cloud.vision.v1.AsyncBatchAnnotateFilesResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Block;
import com.google.cloud.vision.v1.ColorInfo;
import com.google.cloud.vision.v1.CropHint;
import com.google.cloud.vision.v1.CropHintsAnnotation;
import com.google.cloud.vision.v1.DominantColorsAnnotation;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.FaceAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.GcsDestination;
import com.google.cloud.vision.v1.GcsSource;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageContext;
import com.google.cloud.vision.v1.ImageSource;
import com.google.cloud.vision.v1.InputConfig;
import com.google.cloud.vision.v1.LocalizedObjectAnnotation;
import com.google.cloud.vision.v1.LocationInfo;
import com.google.cloud.vision.v1.OperationMetadata;
import com.google.cloud.vision.v1.OutputConfig;
import com.google.cloud.vision.v1.Page;
import com.google.cloud.vision.v1.Paragraph;
import com.google.cloud.vision.v1.SafeSearchAnnotation;
import com.google.cloud.vision.v1.Symbol;
import com.google.cloud.vision.v1.TextAnnotation;
import com.google.cloud.vision.v1.Vertex;
import com.google.cloud.vision.v1.WebDetection;
import com.google.cloud.vision.v1.WebDetection.WebEntity;
import com.google.cloud.vision.v1.WebDetection.WebImage;
import com.google.cloud.vision.v1.WebDetection.WebLabel;
import com.google.cloud.vision.v1.WebDetection.WebPage;
import com.google.cloud.vision.v1.WebDetectionParams;
import com.google.cloud.vision.v1.Word;

import com.google.protobuf.ByteString;

import com.google.protobuf.util.JsonFormat;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuickstartSample {
  public static void log(Object o) {
    if (o == null)
      return;

    System.out.println(o.toString());
  }

  /**
   * Performs document text detection on a remote image on Google Cloud Storage.
   *
   * @param gcsPath The path to the remote file on Google Cloud Storage to detect
   *                document text on.
   * @param out     A {@link PrintStream} to write the results to.
   * @throws Exception   on errors while closing the client.
   * @throws IOException on Input/Output errors.
   */
  // [START vision_fulltext_detection_gcs]
  public static void detectDocumentTextGcs(String gcsPath, PrintStream out) throws Exception, IOException {
    List<AnnotateImageRequest> requests = new ArrayList<>();

    ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
    Image img = Image.newBuilder().setSource(imgSource).build();
    Feature feat = Feature.newBuilder().setType(Type.DOCUMENT_TEXT_DETECTION).build();
    AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
    requests.add(request);

    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
      List<AnnotateImageResponse> responses = response.getResponsesList();
      client.close();

      for (AnnotateImageResponse res : responses) {
        if (res.hasError()) {
          out.printf("Error: %s\n", res.getError().getMessage());
          return;
        }
        // For full list of available annotations, see http://g.co/cloud/vision/docs
        TextAnnotation annotation = res.getFullTextAnnotation();
        for (Page page : annotation.getPagesList()) {
          String pageText = "";
          for (Block block : page.getBlocksList()) {
            String blockText = "";
            for (Paragraph para : block.getParagraphsList()) {
              String paraText = "";
              for (Word word : para.getWordsList()) {
                String wordText = "";
                for (Symbol symbol : word.getSymbolsList()) {
                  wordText = wordText + symbol.getText();
                  // out.format("Symbol text: %s (confidence: %f)\n", symbol.getText(),
                  // symbol.getConfidence());
                }
                // out.format("Word text: %s (confidence: %f)\n\n", wordText,
                // word.getConfidence());
                paraText = String.format("%s %s", paraText, wordText);
              }
              // Output Example using Paragraph:
              out.println("\nParagraph: \n" + paraText);
              int counter = 0;
              for (Vertex vertex : para.getBoundingBox().getVerticesList()) {
                if (counter % 2 == 0) {
                  out.print("\t");
                }
                out.print("X & Y: " + vertex.getX() + "," + vertex.getY() + ", ");
                if (counter % 2 != 0) {
                  out.print("\n");
                }
                counter++;
              }
              // out.format("Paragraph Confidence: %f\n", para.getConfidence());
              blockText = blockText + paraText;
            }
            // out.println("\nBlock: \n" + blockText);
            pageText = pageText + blockText;
          }
        }
        out.println("\nComplete annotation:");
        out.println(annotation.getText());
      }
    }
  }
  // [END vision_fulltext_detection_gcs]

  // [START vision_text_detection_pdf_gcs]
  /**
   * Performs document text OCR with PDF/TIFF as source files on Google Cloud
   * Storage.
   *
   * @param gcsSourcePath      The path to the remote file on Google Cloud Storage
   *                           to detect document text on.
   * @param gcsDestinationPath The path to the remote file on Google Cloud Storage
   *                           to store the results on.
   * @throws Exception on errors while closing the client.
   */
  public static void detectDocumentsGcs(String gcsSourcePath, String gcsDestinationPath) throws Exception {
    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      List<AsyncAnnotateFileRequest> requests = new ArrayList<>();

      // Set the GCS source path for the remote file.
      GcsSource gcsSource = GcsSource.newBuilder().setUri(gcsSourcePath).build();

      // Create the configuration with the specified MIME (Multipurpose Internet Mail
      // Extensions)
      // types
      InputConfig inputConfig = InputConfig.newBuilder().setMimeType("application/pdf") // Supported MimeTypes:
                                                                                        // "application/pdf",
                                                                                        // "image/tiff"
          .setGcsSource(gcsSource).build();

      // Set the GCS destination path for where to save the results.
      GcsDestination gcsDestination = GcsDestination.newBuilder().setUri(gcsDestinationPath).build();

      // Create the configuration for the output with the batch size.
      // The batch size sets how many pages should be grouped into each json output
      // file.
      OutputConfig outputConfig = OutputConfig.newBuilder().setBatchSize(2).setGcsDestination(gcsDestination).build();

      // Select the Feature required by the vision API
      Feature feature = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();

      // Build the OCR request
      AsyncAnnotateFileRequest request = AsyncAnnotateFileRequest.newBuilder().addFeatures(feature)
          .setInputConfig(inputConfig).setOutputConfig(outputConfig).build();

      requests.add(request);

      // Perform the OCR request
      OperationFuture<AsyncBatchAnnotateFilesResponse, OperationMetadata> response = client
          .asyncBatchAnnotateFilesAsync(requests);

      System.out.println("Waiting for the operation to finish.");

      // Wait for the request to finish. (The result is not used, since the API saves
      // the result to
      // the specified location on GCS.)
      List<AsyncAnnotateFileResponse> result = response.get(180, TimeUnit.SECONDS).getResponsesList();

      // Once the request has completed and the output has been
      // written to GCS, we can list all the output files.
      Storage storage = StorageOptions.getDefaultInstance().getService();

      // Get the destination location from the gcsDestinationPath
      Pattern pattern = Pattern.compile("gs://([^/]+)/(.+)");
      Matcher matcher = pattern.matcher(gcsDestinationPath);

      if (matcher.find()) {
        String bucketName = matcher.group(1);
        String prefix = matcher.group(2);

        // Get the list of objects with the given prefix from the GCS bucket
        Bucket bucket = storage.get(bucketName);
        com.google.api.gax.paging.Page<Blob> pageList = bucket.list(BlobListOption.prefix(prefix));

        Blob firstOutputFile = null;

        // List objects with the given prefix.
        System.out.println("Output files:");
        for (Blob blob : pageList.iterateAll()) {
          System.out.println(blob.getName());

          // Process the first output file from GCS.
          // Since we specified batch size = 2, the first response contains
          // the first two pages of the input file.
          if (firstOutputFile == null) {
            firstOutputFile = blob;
          }
        }

        // Get the contents of the file and convert the JSON contents to an
        // AnnotateFileResponse
        // object. If the Blob is small read all its content in one request
        // (Note: the file is a .json file)
        // Storage guide: https://cloud.google.com/storage/docs/downloading-objects
        String jsonContents = new String(firstOutputFile.getContent());
        Builder builder = AnnotateFileResponse.newBuilder();
        JsonFormat.parser().merge(jsonContents, builder);

        // Build the AnnotateFileResponse object
        AnnotateFileResponse annotateFileResponse = builder.build();

        // Parse through the object to get the actual response for the first page of the
        // input file.
        AnnotateImageResponse annotateImageResponse = annotateFileResponse.getResponses(0);

        // Here we print the full text from the first page.
        // The response contains more information:
        // annotation/pages/blocks/paragraphs/words/symbols
        // including confidence score and bounding boxes
        System.out.format("\nText: %s\n", annotateImageResponse.getFullTextAnnotation().getText());
      } else {
        System.out.println("No MATCH");
      }
    }
  }
  // [END vision_text_detection_pdf_gcs]

  public static void main(String... args) throws Exception {
    // log(args[0]);
    // Instantiates a client
    // try (ImageAnnotatorClient vision = ImageAnnotatorClient.create()) {

    // The path to the image file to annotate
    // String fileName = "./resources/wakeupcat.jpg";

    // Reads the image file into memory
    // Path path = Paths.get(fileName);
    // byte[] data = Files.readAllBytes(path);
    // ByteString imgBytes = ByteString.copyFrom(data);

    // String gcsPath = args[0];

    detectDocumentTextGcs(args[0], System.out);
    // detectDocumentsGcs(args[0], args[0] + "_out.png");
  }
}
// [END vision_quickstart]