terraform {
  required_version = "~> 1.7.5"
  required_providers {
    test = {
      source = "robertograham.github.io/tpp4j/test"
    }
  }
}

provider "test" {

}
