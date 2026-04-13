{ pkgs, ... }: {
  # Select the Nix channel (stable-24.05 is recommended for reliability)
  channel = "stable-24.05";

  # Essential packages for Kotlin, Android, and AI development
  packages = [
    pkgs.kotlin                     # Core Kotlin language
    pkgs.kotlin-native              # For potential performance-heavy LLM bindings
    pkgs.gradle                     # Build system
    pkgs.jdk17                      # Recommended for modern Android apps
    pkgs.android-tools              # For ADB and platform debugging
    pkgs.glib                       # Often required for VRM/3D rendering libraries
  ];

  # Environment variables for your dual LLM workflow
  env = {
    # If using local ONNX or PyTorch models, these help with library paths
    LD_LIBRARY_PATH = "${pkgs.stdenv.cc.cc.lib}/lib";
  };

  idx = {
    # VS Code extensions for Kotlin and AI development
    extensions = [
      "fwcd.kotlin"                 # Standard Kotlin support
      "mathiasfrohlich.kotlin"      # Advanced Kotlin syntax
      "google.idx-ai"               # IDX-specific AI tools
    ];

    # Enable the Android emulator for testing your screen overlay
    previews = {
      enable = true;
      previews = {
        android = {
          manager = "android";
        };
      };
    };

    # Commands to run when the workspace is first created
    workspace.onCreate = {
      # Installs any project-specific dependencies
      install-dependencies = "./gradlew dependencies";
    };
  };
}
