package top.zibin.luban.example;

class ImageBean {
  private String originArg;
  private String thumbArg;
  private String image;

  ImageBean(String originArg, String thumbArg, String image) {
    this.originArg = originArg;
    this.thumbArg = thumbArg;
    this.image = image;
  }

  String getOriginArg() {
    return originArg;
  }

  public void setOriginArg(String originArg) {
    this.originArg = originArg;
  }

  String getThumbArg() {
    return thumbArg;
  }

  public void setThumbArg(String thumbArg) {
    this.thumbArg = thumbArg;
  }

  String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }
}
