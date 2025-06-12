package io.jenkins.plugins.netrise.asset.uploader.model;

/**
 * Define GraphQL queries
 * */
public interface Queries {

    private static String flat(String value) {
        assert value != null;
        return value.replaceAll("\n", "");
    }

    /**
     * Query to prepare asset and get upload url
     * */
    String SUBMIT_ASSET_QUERY = flat("""
            mutation Submit($args: SubmitAssetInput, $fileName: String!) {
              asset {
                submit(args: $args, fileName: $fileName) {
                  uploadUrl
                  uploadId
                }
              }
            }
            """);

    /**
     * Query to validate asset upload status
     * */
    String ASSET_UPLOAD_QUERY = flat("""
            query AssetUpload($args: AssetUploadInput) {
                assetUpload(args: $args) {
                    uploadId
                    assetId
                    uploaded
                }
            }
            """);
}
