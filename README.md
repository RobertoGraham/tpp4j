# tpp4j - Terraform Plugin Protocol for Java

## Testing

1. Inside the root of this repository, compile the tpp4j native image:

   ```shell
   ./gradlew nativeCompile
   ```

1. Add the below configuration to
   your [Terraform CLI Configuration File](https://developer.hashicorp.com/terraform/cli/config/config-file):

   ```hocon
   provider_installation {
     dev_overrides {
       "robertograham.github.io/tpp4j/test" = "<absolute path to tpp4j repository root>/build/native/nativeCompile"
     }
     direct {}
   }
   ```

1. Inside the [terraform/tpp4j-test](terraform/tpp4j-test) directory, create a Terraform execution
   plan:

   ```shell
   TF_LOG=trace terraform plan
   ```
