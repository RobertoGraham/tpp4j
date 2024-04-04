terraform {
  required_version = "~> 1.7.5"
  required_providers {
    tls = {
      source  = "hashicorp/tls"
      version = "4.0.5"
    }
    local = {
      source  = "hashicorp/local"
      version = "2.5.1"
    }
  }
}

resource "tls_private_key" "rsa-4096" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "tls_self_signed_cert" "local" {
  private_key_pem = tls_private_key.rsa-4096.private_key_pem

  validity_period_hours = 8760

  dns_names = ["localhost"]

  subject {
    common_name = "tpp4j"
  }

  allowed_uses = [
    "server_auth",
  ]
}

resource "local_file" "private_key" {
  content  = tls_private_key.rsa-4096.private_key_pem_pkcs8
  filename = "${path.module}/../../src/main/resources/privatekey.pem"
}

resource "local_file" "certificate_chain" {
  content  = tls_self_signed_cert.local.cert_pem
  filename = "${path.module}/../../src/main/resources/certificatechain.pem"
}
