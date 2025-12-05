#!/usr/bin/env bash
# Releases viewmapper to DockerHub with multi-platform support

set -e  # Exit on error
set -u  # Exit on undefined variable

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MCP_SERVER_DIR="${PROJECT_ROOT}/viewmapper-mcp-server"
DOCKERHUB_REPO="robfromboulder/viewmapper-mcp-server"

# Helper functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Validate version format (must be 478 followed by a letter)
validate_version() {
    local version=$1
    if [[ ! "$version" =~ ^478[a-z]$ ]]; then
        log_error "Invalid version format: $version"
        echo "Version must be 478 followed by a single lowercase letter (e.g., 478a, 478b, 478c)"
        exit 1
    fi
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."

    if ! command -v git &> /dev/null; then
        log_error "Git is not installed"
        exit 1
    fi

    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed"
        exit 1
    fi

    # Check if Docker buildx is available
    if ! docker buildx version &> /dev/null; then
        log_error "Docker buildx is not available"
        echo "Please install Docker buildx for multi-platform builds"
        exit 1
    fi

    log_success "All prerequisites satisfied"
}

# Verify git status is clean
verify_git_clean() {
    log_info "Verifying git status..."

    if ! git diff-index --quiet HEAD --; then
        log_error "Git working directory has uncommitted changes"
        echo ""
        echo "Please commit or stash your changes before releasing:"
        git status --short
        exit 1
    fi

    if [ -n "$(git ls-files --others --exclude-standard)" ]; then
        log_error "Git working directory has untracked files"
        echo ""
        echo "Please commit or remove untracked files before releasing:"
        git ls-files --others --exclude-standard
        exit 1
    fi

    log_success "Git working directory is clean"
}

# Run clean script
run_clean() {
    log_info "Running clean script..."
    if ! "${PROJECT_ROOT}/clean.sh"; then
        log_error "Clean script failed"
        exit 1
    fi
}

# Run build script
run_build() {
    log_info "Running build script..."
    if ! "${PROJECT_ROOT}/build.sh"; then
        log_error "Build script failed"
        exit 1
    fi
}

# Push to DockerHub with multi-platform support
push_to_dockerhub() {
    local version=$1
    log_info "Pushing ${DOCKERHUB_REPO}:${version} to DockerHub..."

    cd "${MCP_SERVER_DIR}"

    # Create/use buildx builder for multi-platform builds
    if ! docker buildx inspect viewmapper-builder &> /dev/null; then
        log_info "Creating buildx builder instance..."
        docker buildx create --name viewmapper-builder --use
    else
        docker buildx use viewmapper-builder
    fi

    # Build and push multi-platform image
    log_info "Building and pushing multi-platform image (linux/amd64, linux/arm64)..."
    if docker buildx build \
        --platform linux/amd64,linux/arm64 \
        --provenance=true \
        --sbom=true \
        --build-arg VERSION="${version}" \
        -t "${DOCKERHUB_REPO}:${version}" \
        --no-cache \
        --push \
        .; then
        log_success "Successfully pushed ${DOCKERHUB_REPO}:${version}"
    else
        log_error "Failed to push to DockerHub"
        exit 1
    fi

    cd "${PROJECT_ROOT}"
}

# Create and push git tag
create_git_tag() {
    local version=$1
    log_info "Creating git tag v${version}..."

    if git rev-parse "v${version}" &> /dev/null; then
        log_error "Tag v${version} already exists"
        echo "Use 'git tag -d v${version}' to delete the existing tag if needed"
        exit 1
    fi

    git tag -a "v${version}" -m "Release version ${version}"
    log_success "Created git tag v${version}"

    log_info "Pushing tag to origin..."
    if git push origin "v${version}"; then
        log_success "Pushed tag v${version} to origin"
    else
        log_error "Failed to push tag to origin"
        exit 1
    fi
}

# Display release summary
display_summary() {
    local version=$1
    echo ""
    echo "=========================================="
    log_success "Release ${version} complete!"
    echo "=========================================="
    echo ""
    echo "Released artifacts:"
    echo "  - Docker image: ${DOCKERHUB_REPO}:${version}"
    echo "  - Git tag: v${version}"
    echo ""
    echo "Platforms:"
    echo "  - linux/amd64"
    echo "  - linux/arm64"
    echo ""
    echo "Next steps:"
    echo "  1. Verify the release: docker pull ${DOCKERHUB_REPO}:${version}"
    echo "  2. Create a GitHub release (optional)"
    echo ""
}

# Main execution
main() {
    # Check for version parameter
    if [ $# -ne 1 ]; then
        log_error "Missing version parameter"
        echo "Usage: $0 <version>"
        echo "Example: $0 478c"
        exit 1
    fi

    # Do full rebuild prior to pushing container
    local version=$1
    log_info "Starting release process for version ${version}..."
    validate_version "${version}"
    check_prerequisites
    verify_git_clean
    run_clean
    run_build
    push_to_dockerhub "${version}"
    create_git_tag "${version}"
    display_summary "${version}"
}

# Run main function
main "$@"