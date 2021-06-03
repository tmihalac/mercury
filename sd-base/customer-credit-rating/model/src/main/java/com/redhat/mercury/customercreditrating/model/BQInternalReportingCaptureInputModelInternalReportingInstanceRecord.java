/*
 * Customer Credit Rating
 * This service domain maintains and administers the bank's credit assessment for customers.
 *
 * OpenAPI spec version: 2.0.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.redhat.mercury.customercreditrating.model;

import java.util.Objects;
import java.util.Arrays;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
/**
 * BQInternalReportingCaptureInputModelInternalReportingInstanceRecord
 */

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2021-06-03T15:34:54.132884+02:00[Europe/Madrid]")
public class BQInternalReportingCaptureInputModelInternalReportingInstanceRecord {
  @SerializedName("productServiceActivityReportReference")
  private String productServiceActivityReportReference = null;

  @SerializedName("productServiceActivityReportSchedule")
  private String productServiceActivityReportSchedule = null;

  @SerializedName("productServiceActivityReport")
  private Object productServiceActivityReport = null;

  public BQInternalReportingCaptureInputModelInternalReportingInstanceRecord productServiceActivityReportReference(String productServiceActivityReportReference) {
    this.productServiceActivityReportReference = productServiceActivityReportReference;
    return this;
  }

   /**
   * &#x60;status: Not Mapped&#x60;  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::ISO20022andUNCEFACT::Identifier  general-info: Reference to the internal bank product and service activity report that is used for the credit assessment 
   * @return productServiceActivityReportReference
  **/
  @Schema(example = "748135", description = "`status: Not Mapped`  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::ISO20022andUNCEFACT::Identifier  general-info: Reference to the internal bank product and service activity report that is used for the credit assessment ")
  public String getProductServiceActivityReportReference() {
    return productServiceActivityReportReference;
  }

  public void setProductServiceActivityReportReference(String productServiceActivityReportReference) {
    this.productServiceActivityReportReference = productServiceActivityReportReference;
  }

  public BQInternalReportingCaptureInputModelInternalReportingInstanceRecord productServiceActivityReportSchedule(String productServiceActivityReportSchedule) {
    this.productServiceActivityReportSchedule = productServiceActivityReportSchedule;
    return this;
  }

   /**
   * &#x60;status: Not Mapped&#x60;  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::UNCEFACT::Text  general-info: The schedule for the upload of the data file/report 
   * @return productServiceActivityReportSchedule
  **/
  @Schema(description = "`status: Not Mapped`  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::UNCEFACT::Text  general-info: The schedule for the upload of the data file/report ")
  public String getProductServiceActivityReportSchedule() {
    return productServiceActivityReportSchedule;
  }

  public void setProductServiceActivityReportSchedule(String productServiceActivityReportSchedule) {
    this.productServiceActivityReportSchedule = productServiceActivityReportSchedule;
  }

  public BQInternalReportingCaptureInputModelInternalReportingInstanceRecord productServiceActivityReport(Object productServiceActivityReport) {
    this.productServiceActivityReport = productServiceActivityReport;
    return this;
  }

   /**
   * &#x60;status: Not Mapped&#x60;  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::UNCEFACT::Binary  general-info: The report data file/report of customer production activity 
   * @return productServiceActivityReport
  **/
  @Schema(description = "`status: Not Mapped`  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::UNCEFACT::Binary  general-info: The report data file/report of customer production activity ")
  public Object getProductServiceActivityReport() {
    return productServiceActivityReport;
  }

  public void setProductServiceActivityReport(Object productServiceActivityReport) {
    this.productServiceActivityReport = productServiceActivityReport;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BQInternalReportingCaptureInputModelInternalReportingInstanceRecord bqInternalReportingCaptureInputModelInternalReportingInstanceRecord = (BQInternalReportingCaptureInputModelInternalReportingInstanceRecord) o;
    return Objects.equals(this.productServiceActivityReportReference, bqInternalReportingCaptureInputModelInternalReportingInstanceRecord.productServiceActivityReportReference) &&
        Objects.equals(this.productServiceActivityReportSchedule, bqInternalReportingCaptureInputModelInternalReportingInstanceRecord.productServiceActivityReportSchedule) &&
        Objects.equals(this.productServiceActivityReport, bqInternalReportingCaptureInputModelInternalReportingInstanceRecord.productServiceActivityReport);
  }

  @Override
  public int hashCode() {
    return Objects.hash(productServiceActivityReportReference, productServiceActivityReportSchedule, productServiceActivityReport);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BQInternalReportingCaptureInputModelInternalReportingInstanceRecord {\n");
    
    sb.append("    productServiceActivityReportReference: ").append(toIndentedString(productServiceActivityReportReference)).append("\n");
    sb.append("    productServiceActivityReportSchedule: ").append(toIndentedString(productServiceActivityReportSchedule)).append("\n");
    sb.append("    productServiceActivityReport: ").append(toIndentedString(productServiceActivityReport)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}
