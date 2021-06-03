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
 * CRCustomerCreditRatingStateRetrieveOutputModelCustomerCreditRatingStateInstanceAnalysis
 */

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2021-06-03T15:34:54.132884+02:00[Europe/Madrid]")
public class CRCustomerCreditRatingStateRetrieveOutputModelCustomerCreditRatingStateInstanceAnalysis {
  @SerializedName("customerCreditRatingStateInstanceAnalysisData")
  private String customerCreditRatingStateInstanceAnalysisData = null;

  @SerializedName("customerCreditRatingStateInstanceAnalysisReportType")
  private String customerCreditRatingStateInstanceAnalysisReportType = null;

  @SerializedName("customerCreditRatingStateInstanceAnalysisReport")
  private Object customerCreditRatingStateInstanceAnalysisReport = null;

  public CRCustomerCreditRatingStateRetrieveOutputModelCustomerCreditRatingStateInstanceAnalysis customerCreditRatingStateInstanceAnalysisData(String customerCreditRatingStateInstanceAnalysisData) {
    this.customerCreditRatingStateInstanceAnalysisData = customerCreditRatingStateInstanceAnalysisData;
    return this;
  }

   /**
   * &#x60;status: Not Mapped&#x60;  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::UNCEFACT::Text  general-info: The inputs and results of the instance analysis that can be on-going, periodic and actual and projected 
   * @return customerCreditRatingStateInstanceAnalysisData
  **/
  @Schema(description = "`status: Not Mapped`  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::UNCEFACT::Text  general-info: The inputs and results of the instance analysis that can be on-going, periodic and actual and projected ")
  public String getCustomerCreditRatingStateInstanceAnalysisData() {
    return customerCreditRatingStateInstanceAnalysisData;
  }

  public void setCustomerCreditRatingStateInstanceAnalysisData(String customerCreditRatingStateInstanceAnalysisData) {
    this.customerCreditRatingStateInstanceAnalysisData = customerCreditRatingStateInstanceAnalysisData;
  }

  public CRCustomerCreditRatingStateRetrieveOutputModelCustomerCreditRatingStateInstanceAnalysis customerCreditRatingStateInstanceAnalysisReportType(String customerCreditRatingStateInstanceAnalysisReportType) {
    this.customerCreditRatingStateInstanceAnalysisReportType = customerCreditRatingStateInstanceAnalysisReportType;
    return this;
  }

   /**
   * &#x60;status: Not Mapped&#x60;  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::UNCEFACT::Code  general-info: The type of external performance analysis report available 
   * @return customerCreditRatingStateInstanceAnalysisReportType
  **/
  @Schema(description = "`status: Not Mapped`  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::UNCEFACT::Code  general-info: The type of external performance analysis report available ")
  public String getCustomerCreditRatingStateInstanceAnalysisReportType() {
    return customerCreditRatingStateInstanceAnalysisReportType;
  }

  public void setCustomerCreditRatingStateInstanceAnalysisReportType(String customerCreditRatingStateInstanceAnalysisReportType) {
    this.customerCreditRatingStateInstanceAnalysisReportType = customerCreditRatingStateInstanceAnalysisReportType;
  }

  public CRCustomerCreditRatingStateRetrieveOutputModelCustomerCreditRatingStateInstanceAnalysis customerCreditRatingStateInstanceAnalysisReport(Object customerCreditRatingStateInstanceAnalysisReport) {
    this.customerCreditRatingStateInstanceAnalysisReport = customerCreditRatingStateInstanceAnalysisReport;
    return this;
  }

   /**
   * &#x60;status: Not Mapped&#x60;  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::UNCEFACT::Binary  general-info: The external analysis report in any suitable form including selection filters where appropriate 
   * @return customerCreditRatingStateInstanceAnalysisReport
  **/
  @Schema(description = "`status: Not Mapped`  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::UNCEFACT::Binary  general-info: The external analysis report in any suitable form including selection filters where appropriate ")
  public Object getCustomerCreditRatingStateInstanceAnalysisReport() {
    return customerCreditRatingStateInstanceAnalysisReport;
  }

  public void setCustomerCreditRatingStateInstanceAnalysisReport(Object customerCreditRatingStateInstanceAnalysisReport) {
    this.customerCreditRatingStateInstanceAnalysisReport = customerCreditRatingStateInstanceAnalysisReport;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CRCustomerCreditRatingStateRetrieveOutputModelCustomerCreditRatingStateInstanceAnalysis crCustomerCreditRatingStateRetrieveOutputModelCustomerCreditRatingStateInstanceAnalysis = (CRCustomerCreditRatingStateRetrieveOutputModelCustomerCreditRatingStateInstanceAnalysis) o;
    return Objects.equals(this.customerCreditRatingStateInstanceAnalysisData, crCustomerCreditRatingStateRetrieveOutputModelCustomerCreditRatingStateInstanceAnalysis.customerCreditRatingStateInstanceAnalysisData) &&
        Objects.equals(this.customerCreditRatingStateInstanceAnalysisReportType, crCustomerCreditRatingStateRetrieveOutputModelCustomerCreditRatingStateInstanceAnalysis.customerCreditRatingStateInstanceAnalysisReportType) &&
        Objects.equals(this.customerCreditRatingStateInstanceAnalysisReport, crCustomerCreditRatingStateRetrieveOutputModelCustomerCreditRatingStateInstanceAnalysis.customerCreditRatingStateInstanceAnalysisReport);
  }

  @Override
  public int hashCode() {
    return Objects.hash(customerCreditRatingStateInstanceAnalysisData, customerCreditRatingStateInstanceAnalysisReportType, customerCreditRatingStateInstanceAnalysisReport);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CRCustomerCreditRatingStateRetrieveOutputModelCustomerCreditRatingStateInstanceAnalysis {\n");
    
    sb.append("    customerCreditRatingStateInstanceAnalysisData: ").append(toIndentedString(customerCreditRatingStateInstanceAnalysisData)).append("\n");
    sb.append("    customerCreditRatingStateInstanceAnalysisReportType: ").append(toIndentedString(customerCreditRatingStateInstanceAnalysisReportType)).append("\n");
    sb.append("    customerCreditRatingStateInstanceAnalysisReport: ").append(toIndentedString(customerCreditRatingStateInstanceAnalysisReport)).append("\n");
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
